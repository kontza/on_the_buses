# Introduction
This app contains my trials with Spring Cloud Consul Bus.

# How to Run
1. Start a Consul instance. Podman, Docker, standalone, whatnot.
2. Start first instance of this app.
3. Start a second instance of this app (in different port; 4110 is the default).
4. Trigger a message sending via GET http://127.0.0.1:4110/notify .
