// Run with: node --test 'ipt-app/src/test/js/*.test.js'
const test = require('node:test');
const assert = require('node:assert');
const { csrfPrefilter } = require('../../main/resources/static/js/csrf.js');

function headersSentFor(options) {
  const headers = {};
  csrfPrefilter('X-CSRF-TOKEN', 'secret')(options, {}, { setRequestHeader: (name, value) => (headers[name] = value) });
  return headers;
}

test('adds the token to same-origin state-changing requests', () => {
  assert.deepStrictEqual(headersSentFor({ type: 'POST', crossDomain: false }), { 'X-CSRF-TOKEN': 'secret' });
  assert.deepStrictEqual(headersSentFor({ type: 'delete', crossDomain: false }), { 'X-CSRF-TOKEN': 'secret' });
});

test('never sends the token to other origins', () => {
  assert.deepStrictEqual(headersSentFor({ type: 'POST', crossDomain: true }), {});
  assert.deepStrictEqual(headersSentFor({ type: 'GET', crossDomain: true }), {});
});

test('leaves safe requests alone, so cross-origin GETs need no CORS preflight', () => {
  assert.deepStrictEqual(headersSentFor({ type: 'GET', crossDomain: false }), {});
  assert.deepStrictEqual(headersSentFor({ crossDomain: false }), {});
});

const { postLinkForm } = require('../../main/resources/static/js/csrf.js');

function fakeDocument() {
  const make = (tag) => ({ tag, children: [], attrs: {}, appendChild(c) { this.children.push(c); } });
  return { baseURI: 'https://ipt.example.org/manage/resource.do?r=res1', createElement: make };
}

test('turns a confirmed link into a POST form with the token', () => {
  const form = postLinkForm(fakeDocument(), 'delete-source.do?r=res1&id=occ%20a', '_csrf', 'secret');
  assert.strictEqual(form.method, 'post');
  assert.strictEqual(form.action, 'https://ipt.example.org/manage/delete-source.do');
  const fields = Object.fromEntries(form.children.map((c) => [c.name, c.value]));
  assert.deepStrictEqual(fields, { r: 'res1', id: 'occ a', _csrf: 'secret' });
  assert.ok(form.children.every((c) => c.type === 'hidden'));
});
