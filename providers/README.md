# Provider Folder 📁

Please Add You Constum Providers In Here , As .jar Files .
Because , We Bind This Folder Into Keycloak Container .

Check docker-compose.yml :

``` yml
 volumes:
    .
    .
    - ./providers:/opt/keycloak/providers 
```