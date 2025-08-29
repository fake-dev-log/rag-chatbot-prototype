FROM node:22-alpine AS client-fetch

ARG BUILD_MODE

ENV LANG='C.UTF-8' \
    LC_ALL='C-UTF-8'

COPY package.json /app/

WORKDIR /app/

RUN yarn --frozen-lockfile

FROM client-fetch AS client

COPY . /app/

RUN yarn run build

FROM nginx:mainline-alpine
COPY nginx/default.conf /etc/nginx/conf.d/default.conf
COPY --from=client /app/dist /usr/share/nginx/html