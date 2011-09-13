all: set_touchmode

run: set_touchmode
	./set_touchmode /dev/`./find_hidraw.sh`

set_touchmode: set_touchmode.c
	$(CC) -o set_touchmode set_touchmode.c

clean:
	rm -f set_touchmode
