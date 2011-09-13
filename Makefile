all: set_touchmode

run: set_touchmode
	./set_touchmode /dev/`./find_hidraw.sh`

visualise: Visualiser/bin/Main.class
	java -cp Visualiser/bin Main

set_touchmode: set_touchmode.c
	$(CC) -o set_touchmode set_touchmode.c

Visualiser/bin/Main.class: Visualiser/src/Main.java
	javac -d Visualiser/bin Visualiser/src/Main.java

clean:
	rm -f set_touchmode
