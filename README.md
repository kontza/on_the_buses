# Introduction
This app contains my trials with Spring Cloud Consul Bus.

# How to Run
1. Install Consul & start it:

    ```sh
    $ consul agent --dev
    ```
1. Start the gateway:

    ```sh
    $ mvn spring-boot:run -pl :on_the_buses_gw
    ```
3. Start the first instance of this app:

    ```sh
    $ SERVER_PORT=4110 mvn spring-boot:run -pl :on_the_buses_svc
    ```
1. Start the second instance of this app:

    ```sh
    $ SERVER_PORT=4111 mvn spring-boot:run -pl :on_the_buses_svc
    ```
1. Launch the front-end client:

    ```sh
    $ cd vue-client
    $ npm install && npm run dev
    ```
1. Open your browser to http://localhost:4040 to use the front-end client.
