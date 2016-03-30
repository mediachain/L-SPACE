import cytoscape from 'cytoscape';
import cycola from 'cytoscape-cola';

// register the cola layout with cytoscape
cycola(cytoscape, global.cola);

const ColaOpts = {
  avoidOverlaps: true,
  flow: {axis: 'y', minSeparation: 120 },
  nodeSpacing: 120,
  edgeLengthVal: 80,
  animate: true,
  randomize: false,
  maxSimulationTime: 1500
};

const StaticOpts = {
  layout: { name: 'cola', ...ColaOpts },

  // initial viewport state:
  zoom: 1,
  pan: { x: 0, y: 0 },

  // interaction options:
  minZoom: 1e-50,
  maxZoom: 1e50,
  zoomingEnabled: true,
  userZoomingEnabled: true,
  panningEnabled: true,
  userPanningEnabled: true,
  boxSelectionEnabled: false,
  selectionType: 'single',
  touchTapThreshold: 8,
  desktopTapThreshold: 4,
  autolock: false,
  autoungrabify: false,
  autounselectify: false,

  // rendering options:
  headless: false,
  styleEnabled: true,
  hideEdgesOnViewport: false,
  hideLabelsOnViewport: false,
  textureOnViewport: false,
  motionBlur: false,
  motionBlurOpacity: 0.2,
  wheelSensitivity: 1,
  pixelRatio: 'auto'
};

const DefaultStyles = [ // the stylesheet for the graph
  {
    selector: 'node',
    style: {
      'background-color': '#807FD0',
      'label': 'data(label)',
      'width': 120,
      'height': 120,
      'text-valign': 'center',
      'text-wrap': 'wrap',
      'text-max-width': 100
    }
  },

  {
    selector: '.Canonical',
    style: {
      'background-color': '#F96E48',
      'shape': 'rectangle',
      'label': function(node) {
        return node[0]
          .data('canonicalID')
          .split('-')
          .join('- ')
      }
    }
  },

  {
    selector: '.ImageBlob',
    style: {
      'background-color': '#C7DBAB',
      'label': 'data(title)'
    }
  },

  {
    selector: '.Person',
    style: {
      'background-color': '#B4ECE5',
      'label': 'data(name)'
    }
  },

  {
    selector: '.RawMetadataBlob',
    style: {
      'background-color': '#5E5479',
      'label': 'Raw Metadata'
    }
  },

  {
    selector: 'edge',
    style: {
      label: 'data(label)',
      'width': 3,
      'line-color': '#ccc',
      'target-arrow-color': '#ccc',
      'target-arrow-shape': 'triangle'
    }
  }
];

function initCytoscape(containerId, elements, styles) {
  const opts = {
    ...StaticOpts,
    container: document.getElementById(containerId),
    elements: elements,
    style: (styles || DefaultStyles)
  };

  return cytoscape(opts);
};

export default initCytoscape;
