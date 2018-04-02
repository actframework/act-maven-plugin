# ACT Maven Plugin

Enable run act framework application.

## Usage

First, add the plugin into your `pom.xml` file in the `<build><plugins>`:

```xml
<plugin>
    <groupId>org.actframework</groupId>
    <artifactId>act-maven-plugin</artifactId>
    <version>${act-maven-plugin.version}</version>
</plugin>
```

Second, ensure you have `app.entry` property defined in your `pom.xml` file:

```xml
<properties>
    <app.entry>demo.helloworld.AppEntry</app.entry>
</properties>
```

Now you can run your actframework application by

```
mvn clean compile act:run
```

## Configuration

TBD
