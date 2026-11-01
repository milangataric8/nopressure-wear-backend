# Production CORS configuration

Set these AWS Systems Manager Parameter Store values (type `String`):

| Parameter | Value |
| --- | --- |
| `/nopressure/prod/FRONTEND_URL` | `https://nopressurewear.rs` |
| `/nopressure/prod/FRONTEND_ALLOWED_ORIGINS` | `https://nopressurewear.rs,https://www.nopressurewear.rs` |

The production `load-env-from-ssm.sh` is maintained outside this checkout.
Add `FRONTEND_ALLOWED_ORIGINS` to its parameter list so it is written to the
deployment `.env`, and ensure the backend container receives that variable.
Keep `FRONTEND_URL` as the canonical apex URL. Do not print `.env` or secret values.
If the new variable is absent, CORS falls back to `FRONTEND_URL`.

Deploy through the existing GitHub Actions / ECR / SSM pipeline. Do not build
Maven artifacts or Docker images on EC2.

After deployment, verify both origins:

```bash
for origin in https://nopressurewear.rs https://www.nopressurewear.rs; do
  curl -i -X OPTIONS https://api.nopressurewear.rs/api/auth/forgot-password \
    -H "Origin: $origin" \
    -H 'Access-Control-Request-Method: POST' \
    -H 'Access-Control-Request-Headers: content-type'
done
```

Each response must succeed and return `Access-Control-Allow-Origin` matching
the request origin. Repeat with `https://example.com` and confirm rejection.
Check the production health endpoint remains `UP`, then test Forgot Password
in the browser. If email delivery fails, inspect `/nopressure/prod/backend`
in CloudWatch for the actual backend/Brevo error.
