#!/bin/bash
cd ~/OpenVPN-GUI
mvn clean compile
java --module-path /usr/share/openjfx/lib \
     -cp target/classes \
     iq.linux.ova.OpenVPNApp
