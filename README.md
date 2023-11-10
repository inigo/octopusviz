octopus-energyviz retrieves gas and electricity usage from Octopus Energy. Ideally, it would help with "energy disaggregation".

### Usage

This is a normal sbt project. You can compile code with `sbt compile`, run it with `sbt run`, and `sbt console` will start a Scala 3 REPL.

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

with BrowserSync:

> npm install -g browser-sync

and then

> browser-sync start --config browsersync-config.js

will launch a browser, with injected BrowserSync JS, to automatically reload when there are changes.
