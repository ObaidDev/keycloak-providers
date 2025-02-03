FROM quay.io/keycloak/keycloak:26.1.0

# Copy custom SPI providers
COPY ./providers/*.jar /opt/keycloak/providers/
