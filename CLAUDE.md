# NoPressure Wear — WebShop Application

## Project Overview

Full-stack e-commerce web application built as a learning project. Features product management, shopping cart, order processing, online payments, multi-language support, and a comprehensive admin panel.

---

## Tech Stack

### Backend
- **Java 21** with **Spring Boot 3.4.5**
- **Maven** for build management
- **PostgreSQL** as database
- **Flyway** for database migrations
- **Spring Security** with JWT authentication (jjwt 0.12.6)
- **OAuth2** — Google and Facebook login
- **Lombok** for boilerplate reduction
- **Stripe** for online card payments
- **Mailtrap** for email testing (sandbox.smtp.mailtrap.io)
- **Spring WebFlux** (WebClient for external API calls)

### Frontend
- **React 18+** with **Vite** build tool
- **Tailwind CSS** for styling
- **Axios** for HTTP requests
- **React Router** for navigation
- **React Toastify** for notifications
- **react-i18next** for internationalization (EN/SR)
- **@stripe/react-stripe-js** for payment UI

---

## Repository Structure

```
nopressure-wear/
├── webshop-core-backend/     # Spring Boot backend
│   ├── src/main/java/rs/webshop/webshop_core/
│   │   ├── config/           # WebConfig, SecurityConfig
│   │   ├── constants/        # OrderStatus enum
│   │   ├── controller/       # REST controllers
│   │   ├── dto/              # Request/Response DTOs
│   │   │   ├── auth/
│   │   │   ├── banner/
│   │   │   ├── category/
│   │   │   ├── coupon/
│   │   │   ├── filter/
│   │   │   ├── payment/
│   │   │   ├── popup/
│   │   │   ├── product/
│   │   │   ├── setting/
│   │   │   ├── translation/
│   │   │   └── user/
│   │   ├── exception/        # GlobalExceptionHandler, custom exceptions
│   │   ├── model/            # JPA entities
│   │   ├── repository/       # Spring Data JPA repositories
│   │   ├── security/         # JWT, OAuth2, filters
│   │   └── service/          # Business logic
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   └── db/migration/     # Flyway SQL migrations (V1-V22+)
│   └── uploads/products/     # Uploaded product images/videos
│
└── webshop-frontend/         # React frontend
    ├── src/
    │   ├── api/              # Axios API modules
    │   ├── components/
    │   │   ├── admin/        # AdminSearchFilter, AdminPageHeader, Pagination
    │   │   └── common/       # Navbar, Footer, HeroBanner, HomePopup, etc.
    │   ├── context/          # AuthContext
    │   ├── hooks/            # useAuth, useTranslatedContent
    │   ├── i18n/             # en.json, sr.json, i18n.js
    │   ├── pages/
    │   │   ├── admin/        # All admin pages
    │   │   └── *.jsx         # Public pages
    │   └── utils/            # imageUtils, passwordUtils
    ├── .env                  # VITE_API_URL, VITE_STRIPE_PUBLISHABLE_KEY
    └── index.css             # Custom styles, toast overrides
```

---

## Database Entities

### Core Entities
| Entity | Table | Description |
|--------|-------|-------------|
| **User** | `users` | firstName, lastName, email, password, role (ADMIN/EMPLOYEE/CUSTOMER), isActive, stripeCustomerId |
| **Address** | `address` | street, city, postalCode, country, userId (FK) |
| **Category** | `category` | name, description, parentId (self-referencing), isActive |
| **Product** | `product` | name, description, price, stockQuantity, sku, imageUrl, videoUrl, colorName, colorHex, brand, isActive, categoryId |
| **ProductImage** | `product_image` | imageUrl, displayOrder, isPrimary, productId |
| **ProductColorVariant** | `product_color_variant` | productId, variantId (auto-linked by SKU) |
| **ProductAttribute** | `product_attribute` | key, value, productId (dynamic attributes) |

### Order & Cart
| Entity | Table | Description |
|--------|-------|-------------|
| **Order** | `orders` | orderCode (UUID 8 chars, @PrePersist), customerFullName, customerEmail, shippingAddress (embedded), status, totalAmount, discountAmount, couponCode, paymentMethod, paymentStatus, stripePaymentId |
| **OrderItem** | `order_item` | quantity, priceAtPurchase, productId, orderId |
| **Cart** | `cart` | userId |
| **CartItem** | `cart_item` | quantity, cartId, productId |

### Promotions & Content
| Entity | Table | Description |
|--------|-------|-------------|
| **Coupon** | `coupon` | code, discountType (PERCENTAGE/FIXED), discountValue, usageLimit, usageCount, isActive, expiresAt |
| **Banner** | `banner` | title, subtitle, mediaUrl, mediaType (IMAGE/VIDEO), buttonText, buttonLink, displayOrder, isActive |
| **Popup** | `popup` | title, subtitle, content, mediaUrl, mediaType, buttonText, buttonLink, backgroundColor, textColor, isActive, showOnce |

### Configuration
| Entity | Table | Description |
|--------|-------|-------------|
| **StoreSetting** | `store_setting` | key, value, label (store name, logo, footer info, payment toggles) |
| **FilterConfig** | `filter_config` | fieldName, displayName, filterType (select/color/range), isVisible, displayOrder |
| **Translation** | `translation` | entityType, entityId, fieldName, language, value |

---

## Security Architecture

### Dual Filter Chain
- **`@Order(1)` — API Filter Chain** (`/api/**`): Stateless JWT authentication, `JwtAuthEntryPoint` returns 401 (prevents OAuth2 redirect for API calls)
- **`@Order(2)` — OAuth2 Filter Chain** (`/oauth2/**`, `/login/oauth2/**`): Handles Google/Facebook OAuth2 login flow

### Roles & Permissions
| Role | Permissions |
|------|-------------|
| **ADMIN** | Full access to everything including employee management and store settings |
| **EMPLOYEE** | Same as admin except: no employee CRUD, no settings access |
| **CUSTOMER** | Browse products, manage cart, place orders, manage own profile/addresses |

### Key Security Decisions
- `@PreAuthorize` with `@authUtil` for service-level authorization
- `JwtAuthEntryPoint` returns 401 JSON instead of OAuth2 redirect
- SKU is NOT unique (color variants share same SKU)
- `isActive` → `active` in all Response DTOs (Jackson serialization fix)
- Native SQL queries for complex filters (PostgreSQL `LOWER(bytea)` workaround)

---

## API Endpoints

### Public
- `GET /api/products/active` — Browse active products with filters
- `GET /api/products/{id}` — Product detail
- `GET /api/products/filters` — Available filter values (brands, colors, attributes)
- `GET /api/categories/active` — Active categories
- `GET /api/banners/active` — Active banners
- `GET /api/popups/active` — Active popup
- `GET /api/settings/map` — Store settings
- `GET /api/filters/visible` — Visible filter configuration
- `GET /api/translations/{type}/{id}/{lang}` — Translations
- `POST /api/auth/register` — Register
- `POST /api/auth/login` — Login
- `POST /api/auth/forgot-password` — Password reset email
- `POST /api/auth/reset-password` — Reset password with token

### Authenticated
- `GET/POST/PUT/DELETE /api/cart/**` — Cart operations
- `GET /api/orders/{userId}` — User's orders
- `POST /api/orders/{userId}/checkout` — Checkout with optional couponCode and paymentMethod params
- `POST /api/payments/create-payment-intent` — Stripe payment
- `GET/POST/PUT /api/users/**` — Profile management
- `GET/POST/PUT/DELETE /api/addresses/**` — Address management
- `POST /api/coupons/validate` — Validate coupon

### Admin/Employee
- `GET/POST/PUT/PATCH /api/products/**` — Product CRUD + images, color variants, attributes
- `GET/POST/PUT/PATCH /api/categories/**` — Category CRUD
- `GET/PATCH /api/orders/all` — All orders with filters
- `GET/POST/PUT/DELETE /api/coupons/**` — Coupon CRUD
- `GET/POST/PUT/DELETE /api/banners/**` — Banner CRUD
- `POST /api/upload/image` — Image upload (with optional removeBackground param)
- `POST /api/upload/video` — Video upload
- `POST /api/translations/**` — Manage translations

### Admin Only
- `GET/POST/PUT/DELETE /api/employees/**` — Employee CRUD
- `PUT /api/settings/**` — Store settings
- `PUT /api/filters/**` — Filter configuration
- `GET/POST/PUT/DELETE /api/popups/**` — Popup CRUD

---

## Frontend Pages

### Public Pages
| Page | Route | Description |
|------|-------|-------------|
| HomePage | `/` | HeroBanner carousel, featured products, popup |
| ProductsPage | `/products` | Filter panel (categories, color, brand, price), sort, search, grid with pagination |
| ProductDetailPage | `/products/:id` | Image gallery, video, color variants, add to cart |
| LoginPage | `/login` | Email/password + Google/Facebook OAuth2 |
| RegisterPage | `/register` | Registration with password strength validation |
| ForgotPasswordPage | `/forgot-password` | Email input for reset link |
| ResetPasswordPage | `/reset-password` | New password form |
| OAuth2CallbackPage | `/oauth2/callback` | Handles OAuth2 redirect with token |
| NotFoundPage | `*` | 404 page |

### Authenticated Pages
| Page | Route | Description |
|------|-------|-------------|
| CartPage | `/cart` | Items, coupon, shipping address, payment method (Card/COD), Stripe checkout |
| OrdersPage | `/orders` | Order list with pagination |
| OrderDetailPage | `/orders/:id` | Status tracker, items, summary, shipping info, payment info |
| ProfilePage | `/profile` | Personal info, addresses preview, saved payment cards |
| AddressPage | `/addresses` | Address CRUD |
| ChangePasswordPage | `/change-password` | Password change form |

### Admin Pages
| Page | Route | Description |
|------|-------|-------------|
| AdminDashboard | `/admin` | Links to all sections |
| AdminProducts | `/admin/products` | Product table with search, category/brand/color/active filters |
| AdminCategories | `/admin/categories` | Expandable parent/subcategory table |
| AdminOrders | `/admin/orders` | Orders with status filter buttons, search |
| AdminCustomers | `/admin/customers` | Customer list with search |
| AdminCustomerDetail | `/admin/customers/:id` | Customer info + order history |
| AdminCoupons | `/admin/coupons` | Coupon CRUD with search |
| AdminEmployees | `/admin/employees` | Employee CRUD (admin only) |
| AdminBanners | `/admin/banners` | Banner CRUD with image/video upload |
| AdminPopups | `/admin/popups` | Popup CRUD with preview, color picker |
| AdminSettings | `/admin/settings` | Store info, payment toggles, filter configuration |

---

## Key Components

### Reusable Admin Components
- **AdminSearchFilter** — Active/Inactive toggle + search input (used on all admin pages)
- **AdminPageHeader** — Title + subtitle + action button
- **Pagination** — Prev/Next with page count
- **StatusBadge** — Active/Inactive badge
- **OrderStatusBadge** — Colored status badge (PENDING/CONFIRMED/SHIPPED/DELIVERED/CANCELLED)
- **LoadingSpinner** — Centered spinner with configurable height
- **TranslationEditor** — Multi-language field editor for any entity

### Common Components
- **Navbar** — Two-level: mini navbar (auth, language switcher) + main navbar (links, dropdowns, search, cart icon). Fixed on scroll. Mobile responsive with hamburger menu.
- **Footer** — Dynamic from store settings (name, address, hours, contact, Google Maps embed)
- **HeroBanner** — Carousel with auto-rotate (5s), dots, arrows, image/video support
- **HomePopup** — Modal popup with image/video, customizable colors, show-once option
- **PasswordStrength** — Real-time validation (8+ chars, number, special character)
- **CheckoutForm** — Stripe PaymentElement integration
- **AddCardForm** — Stripe SetupIntent for saving cards
- **TranslatedText** — Renders translated content based on current language
- **ImageUpload** — File upload with preview
- **Skeleton** — Loading skeleton placeholder

---

## Key Design Decisions

### Backend
1. **Flyway migrations** — Versioned SQL files (V1-V22+) for database schema management
2. **Native queries** — Used for complex filters because PostgreSQL can't compare `null` with enum types in JPQL
3. **Soft delete** — Products and categories use `isActive` flag instead of hard delete (preserves order history)
4. **Auto-linking color variants** — Products with same SKU and different colors are automatically linked
5. **Predefined color palette** — Standardized colors to prevent filter fragmentation
6. **Dynamic product attributes** — Key-value `product_attribute` table for custom filterable properties
7. **Store settings as key-value** — `store_setting` table for admin-configurable store info
8. **Filter configuration** — `filter_config` table controls which filters appear on products page
9. **Translation table** — Generic `translation` table for any entity's field in any language
10. **Event-driven UI updates** — `window.dispatchEvent` for cross-component communication (categories-updated, settings-updated)

### Frontend
1. **`useAuth` hook** separated from `AuthContext` (ESLint react-refresh compliance)
2. **`getImageUrl` utility** — Handles both relative and absolute image URLs
3. **`axiosInstance`** — Centralized API client with JWT interceptor
4. **`useCallback` + dependency arrays** — Prevents stale closures in filter functions
5. **`response.data.content || response.data`** — Handles both Page and List responses from `getCategories`
6. **`e.stopPropagation()`** — Prevents row click when clicking action buttons in tables
7. **`<button>` instead of `<Link>`** inside `<Link>` wrappers — Prevents nested anchor elements
8. **Mobile responsive** — `hidden md:table-cell` for hiding columns, hamburger menu, expandable search

---

## Environment Variables

### Backend (application.yml / IntelliJ env vars)
```
DATABASE_URL=jdbc:postgresql://localhost:5432/webshop
DATABASE_USERNAME=postgres
DATABASE_PASSWORD=your_password
JWT_SECRET=your_jwt_secret
JWT_EXPIRATION=86400000
GOOGLE_CLIENT_ID=your_google_client_id
GOOGLE_CLIENT_SECRET=your_google_client_secret
FACEBOOK_CLIENT_ID=your_facebook_app_id
FACEBOOK_CLIENT_SECRET=your_facebook_app_secret
STRIPE_SECRET_KEY=sk_test_your_key
REMOVEBG_API_KEY=your_removebg_key
```

### Frontend (.env)
```
VITE_API_URL=http://localhost:8080
VITE_STRIPE_PUBLISHABLE_KEY=pk_test_your_key
```

---

## Email Templates (HTML)

1. **Password Reset** — Branded email with "Reset Password" button and fallback URL link
2. **Order Status Update** — Full order details: greeting, items list, shipping address, subtotal, "View Order" button

---

## Tests

### Unit Tests (JUnit 5 + Mockito)
- `CategoryServiceTest` — CRUD operations, duplicate check, not found exception
- `ProductServiceTest` — Create, deactivate, delete, pagination
- `AuthServiceTest` — Register, login, duplicate email, bad credentials

### Integration Tests (MockMvc)
- `CategoryControllerTest` — HTTP status codes, JSON responses, security (401 for unauthenticated)
- `AuthControllerTest` — Register/login validation, response structure

### Test Annotations
- `@ExtendWith(MockitoExtension.class)` — Activates Mockito
- `@MockitoBean` — Creates mock beans (replaces deprecated `@MockBean`)
- `@WithMockUser` — Simulates authenticated user
- `@WebMvcTest` — Tests controller layer only

---

## Common Issues & Solutions

| Issue | Solution |
|-------|----------|
| `isActive` returns wrong JSON field | Use `private Boolean active` in Response DTOs |
| PostgreSQL `LOWER(bytea)` error | Use native queries instead of JPQL for enum comparisons |
| OAuth2 redirects API calls | Dual SecurityFilterChain with `@Order(1)` for API, `@Order(2)` for OAuth2 |
| `categories.filter is not a function` | Use `response.data.content \|\| response.data` to handle Page objects |
| Flyway migration name mismatch | File name must match exactly what's stored in `flyway_schema_history` |
| `<a>` inside `<a>` error | Use `<button>` with `navigate()` instead of nested `<Link>` |
| Filters not updating | Add all filter states to `useCallback` dependency array |
| Cart badge not updating | Call `setCartCount()` after add to cart |
| Toast appearing in corner | Custom CSS to center: `.Toastify__toast-container--top-center` with `transform` |

---

## Stripe Test Cards

| Type | Card Number |
|------|-------------|
| Successful payment | `4242 4242 4242 4242` |
| Requires authentication | `4000 0025 0000 3155` |
| Declined | `4000 0000 0000 9995` |

Expiry: any future date, CVC: any 3 digits, ZIP: any 5 digits

---

## Running the Project

### Backend
```bash
cd webshop-core-backend
# Set environment variables in IntelliJ or export them
mvn spring-boot:run
# Runs on http://localhost:8080
```

### Frontend
```bash
cd webshop-frontend
npm install
npm run dev
# Runs on http://localhost:5173
```

### Database
```bash
# Create PostgreSQL database
createdb webshop
# Flyway migrations run automatically on backend startup
```

### Tests
```bash
cd webshop-core-backend
mvn test
```
