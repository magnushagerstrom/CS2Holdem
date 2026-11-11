# Demo-workern i en container. Samma idé som serverns Dockerfile: bygg i en stor bild,
# kör i en liten. Go bygger en enda fristående fil, så den färdiga bilden är bara några MB.

FROM golang:1.24-alpine AS build
WORKDIR /src
COPY go.mod go.sum ./
RUN go mod download
COPY *.go ./
# CGO_ENABLED=0: ren Go utan C-bibliotek, så filen fungerar i vilken Linux-bild som helst
RUN CGO_ENABLED=0 go build -trimpath -ldflags="-s -w" -o /worker .

FROM alpine:3.22
# ca-certificates: för att kunna prata HTTPS
RUN apk add --no-cache ca-certificates && adduser -S -u 10002 worker
COPY --from=build /worker /usr/local/bin/worker
USER worker
ENTRYPOINT ["/usr/local/bin/worker"]
