# Local password reset verification — 2026-10-31

Observed causes:
- No backend was listening on localhost:8080.
- Local users table was empty: every forgot-password request returned RESOURCE_NOT_FOUND.
- Local frontend previously used the production API; .env.development now versions the localhost:8080 development default.
- Local frontend and the ignored backend frontend-url now default to port 5173. Vite uses strictPort so it cannot silently switch to 5174. FRONTEND_URL can override the backend URL.

Changes:
- Backend: unknown addresses return the same successful response without sending mail; SMTP operations have finite timeouts; local configuration example includes both allowed development origins.
- Frontend: tracked development API URL, trimmed email, conditional success message, messageKey handling and distinguishable connection failure.

Verification:
- Real browser form at http://localhost:5174/forgot-password succeeded for local-reset-test@example.invalid.
- SMTP returned without exception; failed_email contains zero messages for the synthetic recipient. Mailtrap sandbox captures local emails; external mailbox delivery and Mailtrap inbox UI were not inspected.
- Generated token persisted and valid. API reset with that token: 200. Reusing consumed token: 404.
- Real OPTIONS requests: localhost:5173 and localhost:5174 return 200 with matching CORS header; untrusted origin returns 403.
- Unknown address POST: 200 with matching CORS header.
- Backend AuthServiceTest, AuthControllerTest, CorsConfigurationTest: 28 passed.
- Frontend production build and ESLint for ForgotPasswordPage.jsx passed.

Synthetic test account was retained in the local database. Production was not deployed. Existing unrelated working-tree changes were preserved.
