# ── Stage 1: Build ───────────────────────────────────
FROM node:20-alpine AS builder

WORKDIR /app

COPY package*.json ./
RUN npm ci --frozen-lockfile

COPY . .
RUN npm run build

# ── Stage 2: Nginx ────────────────────────────────────
FROM nginx:1.25-alpine

LABEL description="Payment Platform Frontend"

# Copy build output
COPY --from=builder /app/dist /usr/share/nginx/html

# Nginx config for SPA (React Router)
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=5s \
  CMD wget -q --spider http://localhost:80 || exit 1

CMD ["nginx", "-g", "daemon off;"]
