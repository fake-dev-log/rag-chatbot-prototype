FROM node:22-alpine AS client-fetch

ENV LANG='C.UTF-8' \
    LC_ALL='C-UTF-8'

COPY package.json /app/

WORKDIR /app/

RUN yarn --frozen-lockfile

FROM client-fetch AS client

ARG VITE_API_BASE_URL

ENV VITE_API_BASE_URL=${VITE_API_BASE_URL}

COPY . /app/

RUN yarn run build

FROM nginx:mainline-alpine
COPY nginx/default.conf /etc/nginx/conf.d/default.conf
COPY --from=client /app/dist /usr/share/nginx/html