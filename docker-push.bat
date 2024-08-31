set VERSION=0.0.1
docker build -t grozadanut/flexbiz:latest -t grozadanut/flexbiz:%VERSION% .
:: docker push grozadanut/flexbiz:%VERSION%
:: docker push grozadanut/flexbiz:latest
PAUSE