# Marketplace API v1

Base URL: `http://localhost:8081/api/v1` when running locally with Maven, or `http://localhost:7001/api/v1` when started with `./build`.

All API routes use the `/api/v1` prefix. Except for registration, login, public product and post reads, and uploaded images, requests need a JWT. Send it as `Authorization: Bearer <token>`. JSON endpoints use `Content-Type: application/json`; image uploads use `multipart/form-data` with a `file` field.

## Response format

Successful responses use this envelope:

```json
{
  "status": 200,
  "message": "Success",
  "data": {},
  "timestamp": "2026-10-20T10:30:00"
}
```

Create endpoints return HTTP 201. Errors use the same envelope; validation errors put a field-to-message object in `data`. Common HTTP errors are 400 for invalid input, 401 for missing/invalid authentication, 403 for insufficient permission or ownership, 404 for a missing resource, and 409 for a duplicate or conflicting state.

Paged responses have this shape inside `data`:

```json
{
  "items": [],
  "page": 0,
  "size": 20,
  "totalElements": 0,
  "totalPages": 0
}
```

Product and post feed pages also include the active `categories` and `conditions` arrays so a page can render its feed and filters from one API request. `GET /marketplace/options` returns the same option lists for the listing form.

Pass `page` (zero-based), `size` (maximum 100), and optional `sort`, such as `?page=0&size=20&sort=createdAt,desc`.

## Authentication and accounts

| Method | Path                    | Access        | Body / result                              |
| ------ | ----------------------- | ------------- | ------------------------------------------ |
| POST   | `/auth/register`        | Public        | `RegisterRequest` → `AuthResponse`         |
| POST   | `/auth/login`           | Public        | `LoginRequest` → `AuthResponse`            |
| GET    | `/users/me`             | Authenticated | Current `AccountResponse`                  |
| GET    | `/users/me/permissions` | Authenticated | Array of permission-name strings           |
| PATCH  | `/users/me`             | `user:write`  | `UpdateProfileRequest` → `AccountResponse` |
| GET    | `/users/{id}`           | `user:read`   | Public `UserResponse`                      |
| GET    | `/users/{id}/reviews`   | `user:read`   | Page of reviews for seller                 |

New registrations receive `ROLE_USER` and `canSell: true` by default. `AccountResponse` also includes effective `permissions` and the current `canSell` value.

Registration example: `{ "username":"sam", "email":"sam@example.com", "password":"password123", "fullName":"Sam Lee" }`. Login accepts either `{ "email":"sam@example.com", "password":"password123" }` or `{ "username":"sam", "password":"password123" }`.

## Products and reviews

| Method | Path                                      | Access                        | Body / result                                                                                                           |
| ------ | ----------------------------------------- | ----------------------------- | ----------------------------------------------------------------------------------------------------------------------- |
| GET    | `/products`                               | Public                        | Page of products; filters: `keyword`, `category`, `location`, `condition`, `status`, `minPrice`, `maxPrice`, `sellerId` |
| GET    | `/marketplace/options`                    | Public                        | Active category and condition lists for the listing form                                                             |
| GET    | `/products/{id}`                          | Public                        | `ProductDetailResponse` with images, seller, reviews, and totals; optional `postId` adds that listing’s author, comments, and like state |
| POST   | `/products`                               | `product:write`               | `CreateProductRequest` → `Product`                                                                                      |
| PATCH  | `/products/{id}`                          | Owner, `product:write`        | `UpdateProductRequest` → `Product`                                                                                      |
| PATCH  | `/products/{id}/status`                   | Owner, `product:write`        | `{ "status":"SOLD" }` → `Product`                                                                                       |
| DELETE | `/products/{id}`                          | Owner, `product:write`        | Soft-delete product                                                                                                     |
| GET    | `/products/{id}/images`                   | Public                        | Array of `ProductImage` (also included in `ProductResponse.images`)                                                      |
| POST   | `/products/{id}/images`                   | Owner, `product:write`        | Multipart `file` → `ProductImage`                                                                                       |
| PATCH  | `/products/{id}/images/{imageId}/primary` | Owner, `product:write`        | Set primary image                                                                                                       |
| DELETE | `/products/{id}/images/{imageId}`         | Owner, `product:write`        | Delete image                                                                                                            |
| GET    | `/products/{id}/reviews`                  | Public                        | Page of reviews                                                                                                         |
| POST   | `/products/{id}/reviews`                  | `review:write`                | `{ "rating":5, "comment":"Great" }` → `Review`                                                                          |
| DELETE | `/reviews/{id}`                           | Review author, `review:write` | Delete own review                                                                                                       |
| GET    | `/wishlist`                               | `wishlist:write`              | Page of current user's products                                                                                         |
| PUT    | `/wishlist/{productId}`                   | `wishlist:write`              | Add product (idempotent)                                                                                                |
| DELETE | `/wishlist/{productId}`                   | `wishlist:write`              | Remove product                                                                                                          |

`CreateProductRequest`: `{ "title":"Desk", "description":"Wood desk", "price":75.00, "category":"Electronics", "condition":"GOOD", "location":"Phnom Penh" }`. Category names must be active in Management. Condition values are `NEW`, `LIKE_NEW`, `GOOD`, `FAIR`, `POOR`; product statuses are `AVAILABLE`, `SOLD`, `RESERVED` (`DELETED` is managed by DELETE).

Listing creation payload example (send as the `payload` JSON part; attach each referenced image as a repeated `files` multipart part):

```json
{
  "content": "For sale",
  "products": [
    {
      "title": "Desk",
      "description": "Wood desk",
      "price": 75.00,
      "category": "Furniture",
      "condition": "GOOD",
      "location": "Phnom Penh",
      "imageIndexes": [0, 1]
    }
  ]
}
```

Each `imageIndexes` entry selects a zero-based `files` part. The response contains the created post and each product's full `images` list. The API enforces the existing 10 MB per-image and 50 MB per-request limits.

The `imageUrl` returned for an uploaded image is served below `/api/v1/uploads/`.

## Posts and comments

| Method | Path                   | Access                         | Body / result                                                   |
| ------ | ---------------------- | ------------------------------ | --------------------------------------------------------------- |
| GET    | `/posts`               | Public                         | Page of posts with `postByUser`, products, and per-product like/comment totals; removed posts return the text `This listing is no longer available.` and an empty product list |
| GET    | `/posts/{id}`          | Public                         | `PostResponse` with `postByUser` and product details             |
| POST   | `/posts`               | `post:write`                   | `{ "content":"For sale", "productIds":[1,2] }` → `PostResponse` for existing products          |
| POST   | `/posts/listing`       | `post:write`, `product:write`  | Multipart `payload` JSON plus repeated `files` image parts → creates products, images, and post in one request |
| PUT    | `/posts/{id}`          | Post owner, `post:write`       | Same body as create; replaces linked products                   |
| DELETE | `/posts/{id}`          | Post owner, `post:write`       | Soft-delete post                                                |
| PUT    | `/posts/{id}/reaction` | `post:write`                   | `{ "reactionType":"LIKE" }` → reaction                          |
| DELETE | `/posts/{id}/reaction` | `post:write`                   | Remove own reaction                                             |
| GET    | `/posts/{id}/comments` | Public                         | Page of comments                                                |
| POST   | `/posts/{id}/comments` | `comment:write`                | `{ "content":"Interested" }` → comment                          |
| PATCH  | `/comments/{id}`       | Comment owner, `comment:write` | `{ "content":"Updated" }`                                       |
| DELETE | `/comments/{id}`       | Comment owner, `comment:write` | Soft-delete comment                                             |

Reaction values are `LIKE`, `LOVE`, `HAHA`, `WOW`, `SAD`, `ANGRY`. A post's `PostResponse` contains `post`, linked `products`, and `reactionCount`.

## Friends, messages, and notifications

| Method | Path                             | Access                                  | Body / result                                      |
| ------ | -------------------------------- | --------------------------------------- | -------------------------------------------------- |
| GET    | `/friends`                       | `friend:write`                          | Page of accepted friendships                       |
| GET    | `/friends/requests`              | `friend:write`                          | Page of incoming pending requests                  |
| POST   | `/friends/requests/{userId}`     | `friend:write`                          | Send request → `Friendship`                        |
| PATCH  | `/friends/requests/{id}/accept`  | Request recipient, `friend:write`       | Accept request                                     |
| PATCH  | `/friends/requests/{id}/decline` | Request recipient, `friend:write`       | Decline request                                    |
| DELETE | `/friends/{id}`                  | Friendship participant, `friend:write`  | Remove friendship/request                          |
| GET    | `/messages`                      | `message:write`                         | Latest message from each conversation              |
| GET    | `/messages/{userId}`             | `message:write`                         | Page of conversation messages                      |
| POST   | `/messages/{userId}`             | `message:write`                         | `{ "content":"Hello", "productId":1 }` → `Message` |
| PATCH  | `/messages/{userId}/read`        | `message:write`                         | Mark incoming conversation messages read           |
| GET    | `/notifications`                 | `notification:read`                     | Page of own notifications                          |
| PATCH  | `/notifications/{id}/read`       | Notification owner, `notification:read` | Mark one read                                      |
| PATCH  | `/notifications/read-all`        | `notification:read`                     | Mark all own notifications read                    |

## Admin, roles, and permissions

Staff management routes are admitted for staff authorities and then checked against each feature permission. Role and permission configuration requires `admin:access`; user administration requires `user:manage`.

| Method             | Path                                           | Purpose                                                                     |
| ------------------ | ---------------------------------------------- | --------------------------------------------------------------------------- |
| GET                | `/admin/users`                                 | Page of accounts                                                            |
| GET                | `/admin/users/{id}`                            | Read account                                                                |
| PATCH              | `/admin/users/{id}/active`                     | `{ "active":true }`; disable prevents new authentication                    |
| PATCH              | `/admin/users/{id}/selling`                    | `{ "canSell":false }`; prevent the account from publishing or editing listings |
| PATCH              | `/admin/users/{id}/staff-role`                 | `{ "roleName":"ROLE_HR" }`; `admin:access`, keeps the default user role   |
| GET / POST         | `/admin/categories`                            | List categories / create `{ "name":"Home appliances" }`                   |
| PUT / PATCH        | `/admin/categories/{id}` / `/{id}/active`      | Rename or enable/disable a category                                         |
| GET / PATCH        | `/admin/conditions` / `/{code}/active`         | List or enable/disable a supported product condition                       |
| GET / PATCH        | `/admin/posts` / `/admin/posts/{id}/availability` | Review posts, remove from public feeds, or restore                       |
| GET / POST         | `/admin/roles`                                 | List roles / create `{ "name":"ROLE_SELLER", "description":"Seller" }`      |
| GET / PUT / DELETE | `/admin/roles/{id}`                            | Read, update, or delete a role                                              |
| GET                | `/admin/roles/{id}/permissions`                | Read permissions for a role                                                 |
| PUT                | `/admin/roles/{id}/permissions`                | Replace assignments: `{ "ids":[1,2] }`                                      |
| PUT / DELETE       | `/admin/roles/{id}/permissions/{permissionId}` | Add or remove a role permission                                             |
| GET / POST         | `/admin/permissions`                           | List / create `{ "name":"catalog:export", "description":"Export catalog" }` |
| GET / PUT / DELETE | `/admin/permissions/{id}`                      | Read, update, or delete a permission                                        |
| GET                | `/admin/users/{id}/roles`                      | Read assigned roles                                                         |
| GET                | `/admin/users/{id}/permissions`                | Read effective permission names                                             |
| PUT                | `/admin/users/{id}/roles`                      | Replace assignments: `{ "ids":[1] }`                                        |
| PUT / DELETE       | `/admin/users/{id}/roles/{roleId}`             | Add or remove a user role                                                   |

Management roles include `ROLE_HR`, `ROLE_EMPLOYEE`, `ROLE_MANAGER`, and `ROLE_SUPER_ADMIN`. Built-in roles (`ROLE_ADMIN`, `ROLE_USER`) cannot be deleted. The admin role must keep `admin:access` and `user:manage`; users must keep at least one role. Each protected feature checks its matching permission, such as `product:write`, `category:manage`, or `post:moderate`. Removing a post sets `deleted_at`; it does not delete the post or its related records.

## Frontend models

```ts
type ApiResponse<T> = {
  status: number;
  message: string;
  data: T | null;
  timestamp: string;
};

type Page<T> = {
  items: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};

type AuthResponse = { token: string };
type UserResponse = {
  id: number;
  username: string;
  fullName: string | null;
  avatarUrl: string | null;
  bio: string | null;
  location: string | null;
  createdAt: string;
};
type AccountResponse = {
  profile: UserResponse;
  email: string;
  phone: string | null;
  active: boolean;
  canSell: boolean;
  roles: string[];
  permissions: string[];
};
type Product = {
  id: number;
  sellerId: number;
  title: string;
  description: string | null;
  price: number;
  category: string | null;
  condition: ConditionType;
  location: string | null;
  status: ProductStatus;
  createdAt: string;
  updatedAt: string;
  imageUrl: string | null; // compatibility convenience: primary image
  images: ProductImage[];
  seller: UserResponse | null;
  isSaved: boolean;
};
type ProductImage = {
  id: number;
  productId: number;
  imageUrl: string;
  isPrimary: boolean;
  createdAt: string;
};
type Review = {
  id: number;
  productId: number;
  buyerId: number;
  sellerId: number;
  rating: number;
  comment: string | null;
  createdAt: string;
};
type PostResponse = {
  post: ListingPost;
  products: Product[];
  reactionCount: number;
};
type ListingPost = {
  id: number;
  userId: number;
  content: string | null;
  createdAt: string;
  updatedAt: string;
};
type Comment = {
  id: number;
  postId: number;
  userId: number;
  content: string;
  createdAt: string;
  updatedAt: string;
};
type Friendship = {
  id: number;
  requesterId: number;
  addresseeId: number;
  status: "PENDING" | "ACCEPTED" | "DECLINED";
  createdAt: string;
  updatedAt: string;
};
type Message = {
  id: number;
  senderId: number;
  receiverId: number;
  productId: number | null;
  content: string;
  isRead: boolean;
  createdAt: string;
};
type Notification = {
  id: number;
  userId: number;
  type: "COMMENT" | "MESSAGE" | "FRIEND_REQUEST" | "REVIEW" | "LIKE";
  message: string;
  refId: number | null;
  isRead: boolean;
  createdAt: string;
};
type Permission = { id: number; name: string; description: string | null };
type Role = {
  id: number;
  name: string;
  description: string | null;
  permissions: Permission[];
};
type ConditionType = "NEW" | "LIKE_NEW" | "GOOD" | "FAIR" | "POOR";
type ProductStatus = "AVAILABLE" | "SOLD" | "RESERVED" | "DELETED";
```

Use `profile` on `AccountResponse` for public profile fields. A paged API value is `ApiResponse<Page<T>>`; a list that is not paged is `ApiResponse<T[]>`. Use the token from `AuthResponse` for subsequent authenticated requests.

Development reset: `docker exec -i postgres-db psql -U admin -d marketplace -v ON_ERROR_STOP=1 < scripts/reset-database.sql`. The development admin account is username `admin`, password `12345678`, email `admin@example.com`.
