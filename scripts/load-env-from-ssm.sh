#!/usr/bin/env bash
# Run as the backend deployment user in the directory containing the deployment .env.
# Explicit names use ssm:GetParameters, already available on the EC2 SSM role.
set -euo pipefail
set +x
umask 077
export AWS_PAGER=""
export NP_SSM_PATH="${SSM_PATH:-/nopressure/prod/}"
export NP_ENV_FILE="${ENV_FILE:-.env}"
export NP_REGION="${AWS_REGION:-eu-north-1}"

python3 - <<'PY'
import json
import os
import re
import subprocess
import tempfile
from pathlib import Path

prefix = os.environ['NP_SSM_PATH'].rstrip('/') + '/'
names = '''SPRING_PROFILES_ACTIVE POSTGRES_DB POSTGRES_USER POSTGRES_PASSWORD
DATABASE_URL DATABASE_USERNAME DATABASE_PASSWORD FRONTEND_URL BASE_URL JWT_SECRET JWT_EXPIRATION
GOOGLE_CLIENT_ID GOOGLE_CLIENT_SECRET STRIPE_SECRET_KEY STRIPE_WEBHOOK_SECRET
BREVO_API_KEY BREVO_SENDER_EMAIL BREVO_SENDER_NAME REMOVEBG_API_KEY UPLOAD_DIR
CLOUDINARY_ENABLED S3_STORAGE_ENABLED S3_MEDIA_BUCKET S3_MEDIA_PREFIX S3_PUBLIC_BASE_URL'''.split()
parameters = []
for offset in range(0, len(names), 10):
    result = subprocess.run(['aws', 'ssm', 'get-parameters', '--names',
                             *[prefix + name for name in names[offset:offset + 10]],
                             '--with-decryption', '--region', os.environ['NP_REGION'],
                             '--output', 'json', '--no-cli-pager'], capture_output=True, text=True)
    if result.returncode:
        raise SystemExit('SSM environment refresh failed; existing .env was not changed')
    batch = json.loads(result.stdout)
    if batch.get('InvalidParameters'):
        raise SystemExit('Required SSM parameters are missing; existing .env was not changed')
    parameters.extend(batch['Parameters'])
values = {}
for parameter in parameters:
    name = parameter['Name'][len(prefix):]
    if not re.fullmatch(r'[A-Z][A-Z0-9_]*', name):
        raise SystemExit('Unexpected parameter name; existing .env was not changed')
    value = parameter['Value']
    if '\n' in value or '\r' in value or '\x00' in value:
        raise SystemExit('Multiline environment values are unsupported; existing .env was not changed')
    values[name] = value

for flag in ['CLOUDINARY_ENABLED', 'S3_STORAGE_ENABLED']:
    if values.get(flag, '').lower() not in ('true', 'false'):
        raise SystemExit('Storage flags must exist in Parameter Store and be true/false')
if (values['CLOUDINARY_ENABLED'].lower() == 'true') == (values['S3_STORAGE_ENABLED'].lower() == 'true'):
    raise SystemExit('Exactly one production media storage provider must be enabled')
if values['S3_STORAGE_ENABLED'].lower() == 'true':
    for name in ['S3_MEDIA_BUCKET', 'S3_PUBLIC_BASE_URL', 'S3_MEDIA_PREFIX']:
        if not values.get(name, '').strip():
            raise SystemExit('Missing required S3 media configuration in Parameter Store')

# Docker Compose .env supports single-quoted literal values; no shell expansion or eval.
def render(name):
    return name + "='" + values[name].replace("'", "\\'") + "'"

target = Path(os.environ['NP_ENV_FILE']).resolve()
lines = target.read_text().splitlines() if target.exists() else []
output, seen = [], set()
for line in lines:
    match = re.match(r'^\s*(?:export\s+)?([A-Z][A-Z0-9_]*)\s*=', line)
    name = match.group(1) if match else None
    if name in values:
        if name not in seen:
            output.append(render(name))
            seen.add(name)
    else:
        output.append(line)
output.extend(render(name) for name in sorted(values.keys() - seen))
temp_path = None
try:
    with tempfile.NamedTemporaryFile(mode='w', encoding='utf-8', dir=target.parent, delete=False) as temp:
        temp_path = temp.name
        os.chmod(temp_path, 0o600)
        temp.write('\n'.join(output) + '\n')
    os.replace(temp_path, target)
finally:
    if temp_path and os.path.exists(temp_path):
        os.unlink(temp_path)
print('Environment refreshed from Parameter Store (values not logged)')
PY
