FROM node:22-alpine AS build

WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci

COPY . .

ARG VITE_AUTH_API_URL=/
ARG VITE_BANK_API_URL=/
ENV VITE_AUTH_API_URL=${VITE_AUTH_API_URL}
ENV VITE_BANK_API_URL=${VITE_BANK_API_URL}

RUN npm run build

FROM nginx:alpine

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80
