// vars/welcomeMessage.groovy
def call(String userName = 'User') {
    echo "Welcome to the CI/CD Pipeline, ${userName}!"
    echo "Let's ensure a smooth and efficient build process."
}