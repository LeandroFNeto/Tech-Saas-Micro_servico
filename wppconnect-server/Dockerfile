FROM node:22.22.2-alpine AS base
WORKDIR /usr/src/wpp-server
ENV PUPPETEER_SKIP_CHROMIUM_DOWNLOAD=true
ENV HUSKY=0

# Install build dependencies and runtime libraries for sharp
RUN apk update && \
    apk add --no-cache \
    vips \
    vips-dev \
    fftw-dev \
    gcc \
    g++ \
    make \
    libc6-compat \
    pkgconfig \
    python3 \
    && rm -rf /var/cache/apk/*

# To make sure yarn 4 uses node-modules linker
COPY .yarnrc.yml ./

# Copy only package.json to leverage Docker cache
COPY package.json ./
COPY yarn.lock ./
COPY start.sh ./start.sh

# Enable corepack and prepare yarn 4.14.1
RUN corepack enable && \
    corepack prepare yarn@4.14.1 --activate

# A lib 2.3.3 entra via package.json; o lockfile do 2.10.0 ainda aponta 2.1.0.
RUN yarn install

FROM base AS build
WORKDIR /usr/src/wpp-server
COPY . .
RUN yarn install
RUN yarn build

FROM build AS runtime
WORKDIR /usr/src/wpp-server/

# Install runtime dependencies (chromium and vips libraries)
RUN apk add --no-cache \
    chromium \
    vips \
    fftw

EXPOSE 21465
RUN chmod +x /usr/src/wpp-server/start.sh
ENTRYPOINT ["/usr/src/wpp-server/start.sh"]
CMD ["node", "dist/server.js"]
