FROM openjdk:8-jdk as base
WORKDIR /app
COPY .mvn/ .mvn
COPY mvnw pom.xml ./
RUN chmod a+rwx mvnw
COPY src ./src

FROM base as build
COPY mvnw ./
RUN chmod a+rwx mvnw
RUN sed -i 's/\r$//' mvnw
RUN ./mvnw package -Dmaven.test.skip=true

FROM openjdk:8-jdk as production
COPY --from=build /app/target/truthtables-*.jar /truthtables.jar
COPY --from=build /app/target/classes/sql/db_init.sql /test/db_init.sql
CMD ["java", "-jar", "/truthtables.jar"]
