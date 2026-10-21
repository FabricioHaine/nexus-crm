# =============================================================================
# Nexus CRM - imagem da interface web
# =============================================================================
# Build multi-stage: a primeira etapa compila o bundle, a segunda serve os
# arquivos estaticos com um servidor leve, com fallback para o index.html
# (necessario para o roteamento do Angular).

FROM node:24-alpine AS build
WORKDIR /build

COPY package.json package-lock.json* ./
RUN npm ci --no-audit --no-fund || npm install --no-audit --no-fund

COPY . .
RUN npm run build

# -----------------------------------------------------------------------------
FROM nginx:1.27-alpine

COPY --from=build /build/dist/frontend/browser /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
  CMD wget -qO- http://localhost/ >/dev/null || exit 1
