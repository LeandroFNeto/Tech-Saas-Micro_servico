FROM node:24.11.1-alpine AS construcao
WORKDIR /app
RUN apk add --no-cache git \
    && git config --global url."https://github.com/".insteadOf ssh://git@github.com/ \
    && git config --global url."https://github.com/".insteadOf git@github.com:
COPY package.json package-lock.json ./
RUN npm ci --ignore-scripts
COPY tsconfig.json ./
COPY src ./src
RUN npm run build && npm prune --omit=dev

FROM node:24.11.1-alpine
ENV NODE_ENV=production
ENV PORT=21465
ENV BAILEYS_AUTH_DIR=/auth-data
WORKDIR /app
COPY --from=construcao /app/package.json ./package.json
COPY --from=construcao /app/node_modules ./node_modules
COPY --from=construcao /app/dist ./dist
RUN mkdir -p /auth-data && chown -R node:node /auth-data /app
USER node
EXPOSE 21465
CMD ["node", "dist/index.js"]
