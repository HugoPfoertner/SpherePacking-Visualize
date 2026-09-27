import json
import os
ROOT=os.path.dirname(os.path.abspath(__file__))
T=open(os.path.join(ROOT,'template.html')).read()
common_help = """<p>
Enter a number from 1 to 72 in the text field and press "apply" or hit [ENTER].
You will see the first sphere of the packing (parallel projection). Then you
can put next spheres with "(+)", remove last with "(-)" or all with "all". In the select box you
can change the point of view. Attention, the spheres will come in a different order (from
back to front). Selecting "touches" shows lines if a sphere is in touch with another sphere{WALL}.
<p>
If symmetries are available you can select one. Show the different symmetry
operations with the "0" button. {FIXED} means the ball is mapped onto itself.
Spheres of other colors are mapped (e.g. mirrored or rotated) onto
another sphere of the same color.
<p>
You can rotate by mouse drag (or finger drag on touch screens), or by typing <kbd>x</kbd>, <kbd>y</kbd>, <kbd>z</kbd>.
<kbd>+</kbd> and <kbd>-</kbd> or the mouse wheel zoom in or out. You can also use the arrow keys. <kbd>Ctrl</kbd> is finer,
<kbd>Shift</kbd> changes direction. Click on the picture first to give it the keyboard focus.
"keep view" keeps rotation and zoom while spheres are added or removed; "link" (or key <kbd>p</kbd>)
shows a URL that reproduces the current view.
"""
cube_text = common_help.format(WALL=" or the cube walls", FIXED="White") + """<p>
All data were calculated by <a href="https://www.pfoertner.org/">Hugo Pfoertner</a>.
Mathematical questions please to him. (For all packings with n&gt;10 there are no proofs of optimality.)
<p>
Links to the data:
<a href="sequences.txt">coordinates</a>,
<a href="sphincubsyms.txt">symmetry groups</a>.
See also Hugo's <a href="https://www.randomwalk.de/sphere/incube/">Densest Packings of Equal Spheres in a Cube</a>
and <a href="../insphr/spheresinsphr.html">Densest Packing of Spheres in a Sphere</a>.
<p>
<small>Original Java applet (2005) by Martin Erren; re-implemented in JavaScript (HTML5 canvas) in 2026 so that it runs
in current browsers without Java. The original applet and its sources are in <a href="SpherePacking.zip">SpherePacking.zip</a>.</small>
"""
sph_text = common_help.format(WALL=" or with the container", FIXED="White (grayish shaded)") + """<p>
The comprehensive update of data in August 2013 includes the results provided in a private
communication by Liang Yu.
Prepared for visualization by <a href="https://www.pfoertner.org/">Hugo Pfoertner</a>.
Mathematical questions please to him. (For all packings with n&gt;12 there are no proofs of optimality.)
<p>
Links to the data:
<a href="sequences.txt">coordinates</a>,
<a href="sphinsphsyms.txt">symmetry groups</a> and
<a href="https://oeis.org/A084829/a084829.txt">radii of best known solutions up to n=72</a>.
<p>
See also <a href="../incube/spheresincube.html">Densest Packing of Spheres in a Cube</a>.
<p>
<small>Original Java applet by Martin Erren; re-implemented in JavaScript (HTML5 canvas) in 2026 so that it runs
in current browsers without Java.</small>
"""
pages = {
 'spheresincube.html': dict(TITLE='Densest Packing of Spheres in a Cube', EXTRAHEAD='',
     CONFIG=dict(inSphere=False, sequences='sequences.txt', syms='sphincubsyms.txt', eps=0.001), TEXT=cube_text),
 'spheresinsphr.html': dict(TITLE='Densest Packing of Spheres in a Sphere', EXTRAHEAD='<p><i>Last update of the data: Aug 25, 2013</i></p>',
     CONFIG=dict(inSphere=True, sequences='sequences.txt', syms='sphinsphsyms.txt', eps=1e-6), TEXT=sph_text),
}
import os
for fn,c in pages.items():
    h=T
    for k,v in c.items():
        h=h.replace('@@'+k+'@@', json.dumps(v) if k=='CONFIG' else v)
    assert '@@' not in h
    sub='incube' if 'cube' in fn else 'insphr'
    open(os.path.join(ROOT,'..','web',sub,fn),'w').write(h)
print('ok')
