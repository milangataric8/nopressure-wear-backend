# Pre-deployment verification snapshot — 2026-11-01

This snapshot records checks before rollout approval. It is not a live production
status page; consult CloudFormation, GitHub Actions and runtime checks for current state.

Completed:

- Full backend Maven `verify`: 241 tests, 0 failures/errors; executable JAR built.
- Includes S3 upload bytes/MIME/UUID URLs, background removal on/off, video upload,
  owned URL deletion and rejection of foreign/Cloudinary/traversal URLs.
- Production provider selection, invalid flag combinations, missing S3 config and
  unchanged local provider verified through Spring application contexts.
- Upload API authorization/contracts verified with the real Spring security chains.
- Two existing MVC test slices now mock JwtAuthEntryPoint, a required dependency
  added by the previous authentication fix. No auth implementation changed here.
- CloudFormation `validate-template` successful.
- Isolated EC2 loader test used synthetic SSM data and a temporary directory only:
  flags, special characters through Compose dotenv parsing, preserved entries and
  file mode 0600 verified. No production .env or running container changed.
- Frontend getImageUrl already preserves absolute URLs. No frontend code changed.

AWS preparation (not applied):

- Account 851587047309, region eu-north-1.
- Existing instance uses nopressurewear-ec2-ssm-role; IMDSv2 required, hop limit 2.
- Existing deployment script pulls ECR image; it does not build on EC2.
- Change set `s3-media-storage-review` for stack `nopressurewear-media` is
  CREATE_COMPLETE / AVAILABLE, pending execution. It adds 10 resources: bucket,
  bucket policy, OAC, distribution, scoped IAM policy, five SSM String parameters.
- Current ACM certificate covers apex/www, not media.nopressurewear.rs. The prepared
  plan therefore uses a dedicated CloudFront HTTPS domain with no DNS/cert change.

Pending approval/execution:

- Execute the prepared change set and verify resources/SSM values.
- Install reviewed loader on EC2 (backup the existing script first).
- Commit/push backend changes, allow existing CI → ECR → SSM pipeline to deploy.
- Confirm container flags and production health.
- Upload a real image and video through production admin; verify S3 URLs and
  rendering after refresh, and check CloudWatch. The task's live production
  acceptance criteria are NOT yet fulfilled.
