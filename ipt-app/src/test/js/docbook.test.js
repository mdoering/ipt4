// Run with: node --test 'ipt-app/src/test/js/*.test.js'
const test = require('node:test');
const assert = require('node:assert');
const { decodeEntities, docBookFallbackHtml, safeHref } = require('../../main/resources/static/js/docbook/docbook-v2.js');

test('decodes &amp; last, so an attribute is not decoded twice', () => {
  assert.strictEqual(decodeEntities('&lt;ulink url="a&amp;quot; x"&gt;'), '<ulink url="a&quot; x">');
  assert.strictEqual(decodeEntities('A &amp; B'), 'A & B');
});

test('text that is not DocBook is shown as text, never as markup', () => {
  assert.strictEqual(docBookFallbackHtml('<img src=x onerror=alert(1)>'), '&lt;img src=x onerror=alert(1)&gt;');
  assert.strictEqual(docBookFallbackHtml('A & B'), 'A &amp; B');
});

test('links are web or mail links only', () => {
  assert.strictEqual(safeHref('https://gbif.org/a?b=c'), 'https://gbif.org/a?b=c');
  assert.strictEqual(safeHref('mailto:a@b.org'), 'mailto:a@b.org');
  assert.strictEqual(safeHref('javascript:alert(1)'), null);
  assert.strictEqual(safeHref(' JavaScript:alert(1)'), null);
  assert.strictEqual(safeHref(null), null);
});
