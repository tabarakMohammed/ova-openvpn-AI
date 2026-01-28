GUI app for connect to openvpn on Linux writing by AI 
claude

SELECT how_to_use FROM AI: 

# config 
- Edit sudoers file
sudo visudo

- Add this line (replace 'yourusername' with your actual username):
yourusername ALL=(ALL) NOPASSWD: /usr/sbin/openvpn

# build
- Compile the project
: mvn clean compile

- Run the application
: mvn javafx:run

- Package as JAR (optional)
: mvn package
