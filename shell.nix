{ pkgs ? import <nixpkgs> {} }:
pkgs.mkShell {
  name = "java-agent-env";

  buildInputs = [
    pkgs.jdk8
    pkgs.maven
  ];

  shellHook = ''
    echo "Java Agent Development Environment"
    echo "Java version: $(java -version 2>&1 | head -n 1)"
    echo "Maven version: $(mvn -version | head -n 1)"
  '';
}
