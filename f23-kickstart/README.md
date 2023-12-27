This tool exists to allow students to enter propositional logic formulas with the ability to actively test and evaluate their formula's truth tables. Additionally, students of Dr. Cortens will have the ability to access and practice questions created in chapters. To enter a propositional formula use upper case letters (A, B, C, etc) and the included operators below (ex. C v (~A > D)). For more information on problem types and bug reports you can navigate to our help page.

This application was developed to run in a docker container with a supporting mysql database running in a container as well. To run the docker version navigate to the root directory and run ````docker-compose up --build````. You may need to run that as sudo if your docker requires a higher level of access. This wil start the docker containers and deploy your app to localhost/truth-tables

If you want to run it locally without a docker instance you will need to connect to a database. This can be done with a local docker mysql database, or an external one hosted elsewhere, Heroku for example. Once you have your database set up you will need to update the datasource in the application.properties file. You will also need to build the tables. The code for creating the database and tables can be found in src/main/resources/db_init.sql. Once this is done you can run the application.

To run the application you have a few options. This is a maven project, therefore, you can run any maven command from the root directory such as ````./mvnw spring-boot:run````. We have also supplied ````run.sh````, ````test.sh```` and ````build.sh````.

<b>Notes for future Developers</b>

Branching strategies:

Main - used for deployment to BSU servers

Development - used to run locally, useful for implementing and testing changes before they go to main. Build your branch off this one, test locally, PR into main, run docker-compose up --build on the BSU server, your changes are now live. 

