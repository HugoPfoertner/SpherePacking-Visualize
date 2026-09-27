# SpherePacking-Visualize

Interactive visualization of the densest known packings of equal spheres
in a cube and in a sphere (n = 1 … 72). Data by Hugo Pfoertner;
original Java applet (2005) by Martin Erren.

The Java applet (`SpherePacking.java`) no longer runs in current browsers,
because they dropped Java support. This repository now also contains a
JavaScript/HTML5-canvas re-implementation that needs no plugin.

## Layout

| Path | Content |
|---|---|
| `SpherePacking.java` | original applet source (older version, cube only) |
| `web/incube/spheresincube.html` | new viewer for spheres in a cube |
| `web/insphr/spheresinsphr.html` | new viewer for spheres in a sphere |
| `web/*/sequences.txt`, `web/*/sph*syms.txt` | coordinates and symmetry groups, same files as on randomwalk.de |
| `src/template.html`, `src/build.py` | common source of both pages; `python3 src/build.py` regenerates them |

## Use

Put each HTML page into the same directory as its data files
(as on https://www.randomwalk.de/sphere/incube/ and …/insphr/) and open it
over http(s). The page loads `sequences.txt` and the symmetry file from its
own directory; other files can be given with `?sequences=…&syms=…`.
If the page is opened directly from disk, it offers a file picker for the
two data files.

The controls, colors, shading and keyboard keys follow the applet:
n field / apply, all, (+), (−), point of view (+z … −x), transp./opaque,
touches, symmetry group and the operation button. You can rotate by mouse or
finger drag, or with the keys x/y/z and the arrow keys. `+`, `−` and the mouse
wheel zoom. Ctrl gives finer steps and Shift reverses the direction.

The applet's URL parameters still work:
`?n=…&p=…&xyz=…&s=…&op=…&tr=true&tc=true&ax=…&ay=…&az=…&zf=…`.
There are also new ones:

- `rm=` restores the exact rotation matrix.
- `eps=` sets the contact tolerance. The default is 0.001 for the cube, as in the old applet, and 1e‑6 for the sphere, as in the newer applet.

The **link** button (key `p`) shows the URL of the current view.
**keep view** keeps rotation and zoom while spheres are added or removed.

The sphere-container display (white disk, three great circles, contacts
with the container wall) was reconstructed from the compiled newer applet
`insphr/SpherePacking.jar`, whose source was not available.
