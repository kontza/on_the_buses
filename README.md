# Introduction
This app contains my trials with Spring Cloud Consul Bus.

# How to Run
1. Clone git@github.com:kontza/redesigned-journey.git
2. Follow its README's Proper Setup.
3. Copy the Secret ID mentioned there to _on-the-buses-gw_ `application.yaml`.
4. Start the gateway:
    ```sh
    $ mvn spring-boot:run -pl :on_the_buses_gw
    ```

# Obsolete steps
1. Start the first instance of this app:

    ```sh
    $ SERVER_PORT=4110 mvn spring-boot:run -pl :on_the_buses_svc
    ```
2. Start the second instance of this app:

    ```sh
    $ SERVER_PORT=4111 mvn spring-boot:run -pl :on_the_buses_svc
    ```
3. Launch the front-end client:

    ```sh
    $ cd vue-client
    $ npm install && npm run dev
    ```
4. Open your browser to http://localhost:4040 to use the front-end client.
