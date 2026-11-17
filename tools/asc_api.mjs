// App Store Connect API client (JWT, Team or Individual key). Env: ASC_ISSUER_ID, ASC_KEY_ID, ASC_PRIVATE_KEY.
import crypto from 'node:crypto';

// Team keys sign with iss = Issuer ID; Individual keys use sub = "user" and no
// iss. The kind that works is remembered after the first successful call.
let keyKind = process.env.ASC_KEY_KIND || 'team';

function token(kind = keyKind) {
  let k = process.env.ASC_PRIVATE_KEY.trim();
  if (!k.includes('BEGIN')) {
    k = `-----BEGIN PRIVATE KEY-----\n${k.replace(/\s+/g, '')}\n-----END PRIVATE KEY-----\n`;
  }
  const b64u = (o) => Buffer.from(JSON.stringify(o)).toString('base64url');
  const now = Math.floor(Date.now() / 1000);
  // Trimmed: a pasted secret often ends with a space or newline.
  const head = b64u({ alg: 'ES256', kid: process.env.ASC_KEY_ID.trim(), typ: 'JWT' });
  const claims = { iat: now, exp: now + 1200, aud: 'appstoreconnect-v1' };
  if (kind === 'individual') claims.sub = 'user';
  else claims.iss = process.env.ASC_ISSUER_ID.trim();
  const body = b64u(claims);
  const sig = crypto
    .sign('sha256', Buffer.from(`${head}.${body}`), { key: crypto.createPrivateKey(k), dsaEncoding: 'ieee-p1363' })
    .toString('base64url');
  return `${head}.${body}.${sig}`;
}

async function api(method, path, body) {
  const send = (kind) =>
    fetch(`https://api.appstoreconnect.apple.com${path}`, {
      method,
      headers: { Authorization: `Bearer ${token(kind)}`, 'Content-Type': 'application/json' },
      body: body ? JSON.stringify(body) : undefined,
    });
  let r = await send(keyKind);
  if (r.status === 401 && keyKind === 'team') {
    // Maybe an Individual key: same key, the other token form.
    const retry = await send('individual');
    if (retry.status !== 401) {
      keyKind = 'individual';
      console.error('using the key as an Individual key');
      r = retry;
    }
  }
  const text = await r.text();
  if (!r.ok) {
    const detail = (() => {
      try {
        return JSON.parse(text).errors.map((e) => `${e.title}: ${e.detail}`).join('; ');
      } catch {
        return text.slice(0, 300);
      }
    })();
    const hint = r.status === 401
      ? ' (Apple rejected the key: the Issuer ID, Key ID and .p8 file must belong to the same Team key with Admin access; an "Individual" key does not work here)'
      : '';
    throw new Error(`${method} ${path} → ${r.status} ${detail}${hint}`);
  }
  return text ? JSON.parse(text) : null;
}


export { api };
