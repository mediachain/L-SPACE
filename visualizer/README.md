## L-SPACE visualizer

A graph visualizer for mediachain graphs based on [cytoscape.js][cytoscape],
using the [cola layout][cola].

**This is very much a work in progress.**


### Usage / hacking


```bash
npm install
```

Start the webpack dev server:
```bash
npm run dev
```

At the moment, we're serving up static json data that was dumped from a small
L-SPACE graph, which lives in `src/public/elements.json`.  The plan is to
eventually load this data on the fly from the L-SPACE server.

All the interesting bits are in [`src/app/init-cytoscape.js`](src/app/init-cytoscape.js).

[cytoscape]: http://js.cytoscape.org/
[cola]: https://github.com/tgdwyer/WebCola
