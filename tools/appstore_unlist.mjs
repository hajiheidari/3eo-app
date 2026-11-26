// Removes the app from sale in every territory (reversible: set available=true again).
import { api } from './asc_api.mjs';

const BUNDLE_ID = process.env.IOS_BUNDLE_ID || 'com.espadna.seo';
const apps = await api('GET', `/v1/apps?filter[bundleId]=${BUNDLE_ID}&limit=5`);
const app = apps.data.find((a) => a.attributes.bundleId === BUNDLE_ID);
if (!app) throw new Error(`no app ${BUNDLE_ID}`);
const av = (await api('GET', `/v1/apps/${app.id}/appAvailabilityV2`)).data;
console.log(`app ${app.attributes.name} (${app.id}), availability ${av.id}, new territories: ${av.attributes.availableInNewTerritories}`);

let list = [];
let next = `/v2/appAvailabilities/${av.id}/territoryAvailabilities?limit=200`;
while (next) {
  const page = await api('GET', next.replace('https://api.appstoreconnect.apple.com', ''));
  list.push(...page.data);
  next = page.links?.next || null;
}
console.log(`territories: ${list.length}, available now: ${list.filter((t) => t.attributes.available).length}`);

let changed = 0, failed = 0;
for (const t of list.filter((x) => x.attributes.available)) {
  try {
    await api('PATCH', `/v1/territoryAvailabilities/${t.id}`, { data: { type: 'territoryAvailabilities', id: t.id, attributes: { available: false } } });
    changed++;
  } catch (e) {
    failed++;
    console.log(`::warning::${t.id}: ${e.message.slice(0, 140)}`);
  }
}
console.log(`removed from sale in ${changed} territories, ${failed} failed`);

// verify
let left = 0;
next = `/v2/appAvailabilities/${av.id}/territoryAvailabilities?limit=200`;
while (next) {
  const page = await api('GET', next.replace('https://api.appstoreconnect.apple.com', ''));
  left += page.data.filter((t) => t.attributes.available).length;
  next = page.links?.next || null;
}
console.log(`CHECK territories still available: ${left}`);
if (left || failed) process.exit(1);
