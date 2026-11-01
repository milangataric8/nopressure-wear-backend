# Production media storage

## Runtime

Production defaults to `S3_STORAGE_ENABLED=true`, `CLOUDINARY_ENABLED=false`.
Exactly one must be true. Cloudinary's client and config are absent when disabled;
the S3 client uses the AWS default credential chain / EC2 IAM role. Local and CI
continue using `LocalStorageService`. `REMOVEBG_ENABLED` is independent.

Required SSM String parameters are `/nopressure/prod/CLOUDINARY_ENABLED`,
`S3_STORAGE_ENABLED`, `S3_MEDIA_BUCKET`, `S3_MEDIA_PREFIX`, `S3_PUBLIC_BASE_URL`
(the same prefix applies to every name). Region defaults to `eu-north-1`.
S3 URLs use the configured HTTPS base plus a UUID object key. Existing Cloudinary
records are neither changed nor deleted. The frontend already preserves absolute
URLs; no frontend deploy is necessary.

## Prepare infrastructure before deploying this backend

`media-storage.yml` creates a dedicated encrypted, versioned, private S3 bucket,
CloudFront OAC, a read-only distribution, scoped access for the existing EC2 role,
and the five SSM parameters. It retains the bucket if the stack is deleted.
It never grants a role `s3:*` or `ListBucket`; a bucket-policy deny uses `s3:*`
only to prohibit non-TLS traffic. No static credentials or public write access.

Inspect existing resources and parameter names before creating a stack. Existing
unmanaged resources/parameters must be imported or reconciled rather than overwritten.

```bash
aws cloudformation validate-template --template-body file://infrastructure/media-storage.yml
aws cloudformation deploy --region eu-north-1 \
  --stack-name nopressurewear-media \
  --template-file infrastructure/media-storage.yml \
  --capabilities CAPABILITY_NAMED_IAM \
  --parameter-overrides BackendRoleName=nopressurewear-ec2-ssm-role
aws cloudformation describe-stacks --region eu-north-1 \
  --stack-name nopressurewear-media --query 'Stacks[0].Outputs'
```

CloudFront's HTTPS domain works without DNS changes. To use `media.nopressurewear.rs`,
supply both `MediaDomainName` and a matching **issued us-east-1** ACM `CertificateArn`,
then create the DNS alias/CNAME to the `CloudFrontDomain` output before loading
the custom media base into the running backend. No frontend bucket is reused.

## EC2 environment loader

The live `load-env-from-ssm.sh` and `deploy.sh` are maintained outside this checkout.
If that loader has an explicit key list, add the five media names above. Or review
and install `scripts/load-env-from-ssm.sh` in its place (requires AWS CLI and Python 3).
This version loads the existing production key list plus the five media keys from
`/nopressure/prod/` using batched `ssm:GetParameters`, validates the
storage flags, atomically updates `.env`, preserves non-SSM entries and does not
print values. Do not `source` the resulting file; it is a Docker Compose dotenv file.
The role needs its existing SSM read/KMS permissions as well as media S3 permissions.
Compose must pass `.env` via `env_file`; no duplicate `environment` entries needed.

Keep `.env` private (`0600`). Docker containers cannot use a host CLI login; the EC2
instance profile must be reachable from the container's default credential chain
(check IMDSv2/container networking if credentials cannot be resolved).

## Rollout

Create/verify infrastructure and loader first. Deploy the tested backend through
the existing GitHub Actions → ECR → SSM pipeline; never build Maven/Docker on EC2.
Reload the SSM environment during the existing deploy flow. A flag change requires
a container recreation:

```bash
./load-env-from-ssm.sh
docker compose up -d --force-recreate backend
curl --fail http://127.0.0.1:8080/actuator/health
docker exec nopressure-backend printenv \
  CLOUDINARY_ENABLED S3_STORAGE_ENABLED S3_MEDIA_BUCKET S3_MEDIA_PREFIX S3_PUBLIC_BASE_URL
```

Confirm health is UP. In the production admin UI upload an image with background
removal off, confirm HTTP 200, an S3/CloudFront URL, rendering in admin/product view
and after refresh. Repeat for video. Check CloudWatch `/nopressure/prod/backend`.
Failures log provider, operation, exception class and a safe reason/status code.
Never dump all environment values or authorization headers.

To roll back to Cloudinary, set SSM flags `S3_STORAGE_ENABLED=false` and
`CLOUDINARY_ENABLED=true`, ensure the existing Cloudinary credentials are available,
reload `.env` and recreate the container. Keep S3 and historical Cloudinary media.

## Verification status

Local tests use mock S3 clients; no real AWS credentials or uploads are required.
Infrastructure provisioning, Parameter Store changes, CI/deployment and a real
production image/video upload must be verified against the authenticated AWS account
before calling the production migration complete.

References: [AWS SDK v2 Maven BOM](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/setup-project-maven.html),
[CloudFront OAC and S3 bucket policy](https://docs.aws.amazon.com/AmazonCloudFront/latest/DeveloperGuide/private-content-restricting-access-to-s3.html).
