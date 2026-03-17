octopus-energyviz retrieves gas and electricity usage from Octopus Energy. Ideally, it would help with "energy disaggregation".

### Usage

This is a normal sbt project. You can compile code with `sbt compile`, run it with `sbt run`, and `sbt console` will start a Scala 3 REPL.

It will run at http://localhost:8080/ - see the http://localhost:8080/consumption and 
http://localhost:8080/telemetry/yesterday endpoints particularly, and check the ConsumptionRoutes class
to find other endpoints. 

The telemetry endpoint is most useful if you have an Octopus Home Mini so you can get 10s resolution data.

## Postgres configuration

Ensure you have Postgres 13+ installed and running.

Create the required database - e.g. via:

```bash
    > psql -h localhost -U postgres
```

then:

```sql
    create user "energy" with password 'password';
    create database "energy" with owner "energy";
    alter user energy with SUPERUSER;
    quit
```

## API key

Create a file `src/main/resources/local.conf` and copy the "octopus" section of `application.conf` into it, 
replacing the values with those for your Octopus Energy account at 
https://octopus.energy/dashboard/new/accounts/personal-details/api-access.

## Development

To continually reload any changes, use the SBT Revolver plugin:

```
bin\sbt 

> ~ reStart 
```

This will launch a server on port 8080: http://localhost:8080/

Then use BrowserSync:

> npm install -g browser-sync

and then

> browser-sync start --config browsersync-config.js

will launch a browser, with injected BrowserSync JS, to automatically reload when there are changes.

## Usage

There are two separate datasets:

* Consumption - electricity and gas usage, retrieved via the Octopus REST API, with thirty-minute granularity for the gas and five-minute granularity for the electricity
* Telemetry - this is electricity usage data at 10s granularity, retrieved via the GraphQL endpoint. This data is only present if there's a working Octopus Home Mini set up to store the data, and in the API it doesn't go back more than a few weeks.

Both are retrieved by running the EnergyUsageStorerIntegrationTest manually, rather than dynamically. The 
