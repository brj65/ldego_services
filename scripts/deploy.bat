rm -r target
docker image rm -f brj65/ldego_services:latest
docker stop dev-ldego_services
./mvnw clean package -Dquarkus.container-image.build=true  
cp target/infitech-*-SNAPSHOT.jar /opt/docker/back_end/app 
docker start dev-ldego_services
docker push brj65/ldego_services:latest
