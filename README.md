# SpringRESTLoanApplication
A first version of my personal project for Spring with Java-based configuration

This is a Java Web application created using Eclipse IDE and Maven. It is using as most important technologies Hibernate, Spring core 
and Spring MVC - all configuration for the Spring part has been done using Java-based configuration.

Its purpose is to illustrate a possibility of applying for loans of different amounts in CZK or EUR - the maximum available 
amounts of money that can be granted as loan are established inside the application alongside the other application constants.

Each loan is given for an initial period of one week - the client may be able to extend it with another week, case in which its interest 
rate (set initially to 10% of the loaned sum) will get increased with a factor of 1.5.
There are important risk conditions under which each loan application will be undergoing before a successful submission and acceptance:
A) the loaned amount will be checked against the established limits in the application in order to see if it is acceptable + in case the 
   loaned amount is the maximum value, the loan application date and time are also verified for exclusion of a high risk period between 
   midnight and 6 AM every day.
B) the IP address from where the loans are submitted is also currently retained - for now it is assigned to the customer as a separate
   but dependent entity. Its retaining is also necessary in order to evaluate whether the number of loans sent from a particular IP address
   exceeds a certain daily limit (in this case 3, but this is also configurable with respect to the application).
    
The database server used by this application is MySQL - the application also has a front-end part where the RESTful web services listed 
in this are consumed - this is Java desktop client created using the Java FX technology (additional libraries added here include Apache POI and iText PDF for allowing the customer to export his/her loan history into a PDF or Excel document or the Google mail API for sending e-mails during client registration, client update or loan registration and extension for example).

In order to use the current application, first the back-end part will need to be deployed on a web server (ideally it could be a Tomcat server as this will allow as a bonus facility the automated deployment using a Maven-Tomcat plugin).

Next the front-end will be possible to start as a normal Java desktop application - here the user is encouraged to import the application into an IDE where the Java FX facility is incorporated.
