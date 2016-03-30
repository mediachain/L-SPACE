import cytoscape from 'cytoscape';
import cycola from 'cytoscape-cola';
import cola from 'cola';

cycola(cytoscape, cola);

const StaticOpts = {
  layout: { name: 'cola' },

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

function initCytoscape(containerId, elements, style) = {
  const opts = {
    ...StaticOpts,
    container: document.getElementById(containerId),
    elements: elements,
    style: style
  };

  return cytoscape(opts);
};

export default initCytoscape;
