require('isomorphic-fetch');
import initCytoscape from './init-cytoscape';

const containerId = 'app';

const loadElements = fetch('/elements.json').then(response => {
  if (response.status >= 400) {
    throw new Error('Error fetching JSON: ' + response.statusText);
  }
  return response.json();
});

const cy = initCytoscape(containerId, loadElements);

console.log('initialized cytoscape');
