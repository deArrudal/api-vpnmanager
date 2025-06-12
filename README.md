# VPN Manager

## Overview

**VPN Manager** is a Spring Boot web application designed to manage the lifecycle of VPN certificates in an internal, offline network environment. It provides:

- User management (admin role only)
- VPN creation and revocation for users
- Integration with Python scripts to handle certificate generation and revocation
- Secure download of VPN ZIP packages by the user
- HTTPS-secured web interface


## Project Structure

```
vpnmanager/
├── src/
│   └── main/
│       ├── java/br/com/vpnmanager/
│       │   ├── controller/
│       │   │   ├── AdminController.java 
│       │   │   ├── AuthController.java     <-- serves login services
│       │   │   ├── ExportController.java   <-- serves ZIP files
│       │   │   ├── UserController.java
│       │   │   └── VPNController.java      <-- serves admin's vpn pages
│       │   ├── entity/
│       │   │   ├── User.java 
│       │   │   └── VPN.java
│       │   ├── repository/
│       │   │   ├── UserRepository.java 
│       │   │   └── VPNRepository.java
│       │   ├── security/
│       │   │   ├── RoleBasedAuthenticationSuccessHandler.java
│       │   │   └── SecurityConfig.java
│       │   ├── service/
│       │   │   ├── UserDetailsServiceImpl.java
│       │   │   ├── UserService.java
│       │   │   └── VPNService.java         <-- calls Python scripts
│       │   ├── util/
│       │   │   └── Role.java
│       │   └── VpnManagerApplication.java
│       └── resources/
│           ├── templates/
│           │   ├── user/
│           │   │   ├── create.html
│           │   │   ├── edit.html
│           │   │   └── list.html
│           │   ├── vpn/
│           │   │   └── list.html
│           │   └── login.html
│           ├── static/css/
│           │   └── bootstrap.min.css
│           └── application.properties      <-- main config
├── pom.xml
└── README.md

```

## Requirements

* Java 17+ (for Spring Boot 3.x)
* Maven (Optional – if building on server)
* Python 3
* MySQL 8+
* SSL certificate (PKCS12 format)

## Quickstart (Offline Server)

```bash
# On development machine
mvn dependency:go-offline -DincludePluginDependencies=true

# Transfer to server
scp -r vpnmanager/ userlinux@your-vpn-server:/home/userlinux/

# On server
cd /home/userlinux/vpnmanager
./install.sh
```

## Project Configuration

### `application.properties`

Located in `src/main/resources/application.properties`:

```properties
# === Database Configuration ===
spring.datasource.url=jdbc:mysql://{database-server-ip}:3306/vpnmanager
spring.datasource.username={username}
spring.datasource.password={password}


# === Server Port ===
server.port=443
server.ssl.key-store=/opt/vpnmanager/keystore.p12
server.ssl.key-store-password={password}
server.ssl.key-store-type=PKCS12
server.ssl.key-alias=vpnmanager

# === Spring Security Login Page ===
spring.security.user.name={username}
spring.security.user.password={password}

```

> Replace the above information as needed.

### `ExportController.java`

Serves the generated ZIP file using a download endpoint. Relies on:

```java
private static final Path EXPORT_DIR = Paths.get("/opt/easy-rsa/exports/");
```

This should match the output directory used by the Python certificate script.

### `VPNService.java`

Responsible for executing:

* `create_certificate_client.py`
* `revoke_certificate_client.py`

Update the paths to point to:

```java
private static final String CREATE_SCRIPT = "/opt/easy-rsa/create_certificate_client.py";
private static final String REVOKE_SCRIPT = "/opt/easy-rsa/revoke_certificate_client.py";
```

## MySQL Setup

###  Create a user account to connect as remote host

1. SSH into your MySQL VM and run:

    ```sql
    CREATE USER '{username}'@'{remote_server_ip}' IDENTIFIED BY '{password}';
    GRANT CREATE, ALTER, DROP, INSERT, UPDATE, DELETE, SELECT, REFERENCES, RELOAD on *.* TO '{username}'@'{remote_server_ip}' WITH GRANT OPTION;
    CREATE DATABASE vpnmanager;
    FLUSH PRIVILEGES;
    ```

    > This user must match what you configured in application.properties.

    Spring Boot will automatically create the tables based on your entities when spring.jpa.hibernate.ddl-auto=update is set.

### Create an Admin user (After user table is created by springboot)

Since the app doesn’t currently include an user registration or admin seeding logic, you need to manually insert one into MySQL:

1. Hash a password using BCrypt and insert manually. e.g.:

    ```sql
    INSERT INTO user (username, first_name, last_name, email, role, password)
    VALUES ('admin', 'System', 'Admin', 'admin@vpnmanager.com', 'ADMIN', '$2a$12$7jCXyrVWUGKl/KLGoCaEM.Kl1XYhiN1kWvwJSgtJA2C4yXITmJA96');
    ```

## Self-Signed SSL Certificate (Keystore)

### Generate a self-signed certificate

1.  Execute the `keytool` command to create a PKCS12 keystore file (`keystore.p12`):

    ```bash
    keytool -genkeypair -alias vpnmanager -keyalg RSA -keysize 2048 \
      -storetype PKCS12 -keystore keystore.p12 -validity 3650 \
      -storepass {password} -keypass {password} \
      -dname "CN=localhost, OU=VPN, O=MyOrg, L=City, ST=State, C=BR"
    ```
    
    > Remember the `storepass` and `keypass` and the `alias` (`vpnmanager`).

    This creates `keystore.p12` in your current directory.

2.  Move the generated `keystore.p12` file into a secure directory.
    ```bash
    sudo mv keystore.p12 /opt/vpnmanager/keystore/keystore.p12
    ```

3.  Open your `application.properties` file in your Spring Boot project and update the necessary fields.

### Bind Java to Port 443

1.  Locate your Java executable. 
	
    If you're running a JAR, it's typically `/usr/bin/java` or the path to your JDK's `java` binary (e.g., `/opt/jdk-xyz/bin/java`).

2.  Use `setcap` to grant the Java executable the ability to bind to privileged ports:

    ```bash
    sudo setcap 'cap_net_bind_service=+ep' /usr/bin/java
    # Or, if your Java is elsewhere:
    # sudo setcap 'cap_net_bind_service=+ep' /path/to/your/jdk/bin/java
    ```
	
    > Note:** This command might need to be re-applied after Java updates.

    Since you're using a self-signed certificate, your browser will likely show a warning about an untrusted connection. You'll need to accept the risk or add an exception to proceed.

    If using this application in production or enterprise, replace the self-signed certificate with a valid one from your internal CA or certificate authority.

## Installation (Offline Server)

1. Navigate to the project folder and run:

    ```bash
    mvn dependency:go-offline -DincludePluginDependencies=true
    ```

1. Transfer project to the server via `scp` or `rsync`.

2. Run the following script **as `userlinux`** to install:

    `ìnstall.sh`

    ```bash
    #!/bin/bash
    set -e

    # Convert line endings
    find . -type f -name "*.sh" -exec sed -i 's/\r$//' {} +

    # Build the project
    ./mvnw -o clean package -DskipTests

    # Create deployment directory
    mkdir -p /opt/vpnmanager
    cp target/*.jar /opt/vpnmanager/vpnmanager.jar
    chown -R appuser:appuser /opt/vpnmanager
    ```

## systemd Service Example

1. Create `/etc/systemd/system/vpnmanager.service`:

    ```ini
    [Unit]
    Description=VPN Manager Service
    After=network.target

    [Service]
    User=appuser
    Group=certadmins
    UMask=0002
    WorkingDirectory=/opt/vpnmanager
    ExecStart=/usr/bin/java -jar /opt/vpnmanager/vpnmanager.jar
    SuccessExitStatus=143
    Restart=always
    RestartSec=5

    [Install]
    WantedBy=multi-user.target
    ```

2. Enable and start the service:

    ```bash
    sudo systemctl daemon-reexec
    sudo systemctl daemon-reload
    sudo systemctl enable vpnmanager.service
    sudo systemctl start vpnmanager.service
    ```

## Accessing the Application

Visit:

```
https://{server-ip}/
```

> Since this is an internal offline environment, make sure DNS or IP routing is configured as needed.


##  Final Notes

* Ensure that `EXPORT_DIR` and Python script paths are consistent between your properties and service.
* All VPN file downloads are protected and validated so only the user owner (or admin) can access the file.
* To run the application on port 8080, remove the **Server Port** section of the `application.properties` and skip the **`Self-Signed Certificate`** step.
* Some firewall rules can impact the access to the application. Always check your rules using:

    ```bash
    nft list ruleset
    ```

## Authors

 - deArruda, Lucas [@SardinhaArruda](https://twitter.com/SardinhaArruda)

## Version History

* 1.0
    * Initial Release

## License

This project is licensed under the GPL-3.0 License - see the LICENSE.md file for details
