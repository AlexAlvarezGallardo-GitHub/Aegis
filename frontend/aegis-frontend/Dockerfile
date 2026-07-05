FROM node:22-alpine AS build
WORKDIR /app

# ── 1. Cache npm dependencies ──────────────────────────
#     (only invalidated if package.json / lockfile changes)
COPY package.json package-lock.json ./
RUN npm ci

# ── 2. Build app ───────────────────────────────────────
#     (only invalidated if source files change)
COPY . .
RUN npm run build

# ── Runtime ──────────────────────────────────────────────
FROM nginx:stable-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist/aegis-frontend/browser /usr/share/nginx/html
EXPOSE 80
