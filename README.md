# Mail Transfer Monitoring Server

## Using gradle

### Compile & Test

Using gradle:

Compile the project using the gradle wrapper:

    ./gradlew assemble

Compile and run the tests:

    ./gradlew build

### Run the applications

The gradle config contains several tasks that start application components for you.
You can list them with

    ./gradlew tasks --all

And search for 'Other tasks' starting with `run-`.

    ./gradlew --console=plain run-monitoring
