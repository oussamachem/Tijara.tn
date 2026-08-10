// Impression du bordereau de livraison Goodex (fenêtre pop-up), à coller sur le colis.
// Reprend le code de suivi (ean), l'expéditeur (boutique), le destinataire (livraison) et le COD.

const esc = (s) => String(s ?? '').replace(/[&<>]/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;' }[c]));
const dt = (x) => `${Number(x || 0).toFixed(3)} DT`;
const now = () => new Date().toLocaleString('fr-FR', { dateStyle: 'short', timeStyle: 'short' });

/** order : OrderAdminDetailResponse (reference, total, delivery*, carrierEan, items). */
export function printBordereau(shopName, order) {
  const items = (order.items || [])
    .map((it) => `<tr><td>${esc(it.productName)} · ${esc(it.color)}/${esc(it.size)}</td><td class="r">× ${it.quantity}</td></tr>`)
    .join('');
  const body = `
    <div class="row">
      <div class="shop">${esc(shopName || 'Boutique')}</div>
      <div class="tag">BORDEREAU DE LIVRAISON</div>
    </div>
    <div class="ean">${esc(order.carrierEan || '—')}</div>
    <hr>
    <table class="kv">
      <tr><td class="k">Commande</td><td>${esc(order.reference)}</td></tr>
      <tr><td class="k">Date</td><td>${now()}</td></tr>
    </table>
    <div class="box">
      <div class="lbl">Destinataire</div>
      <div class="big">${esc(order.deliveryName || '—')}</div>
      <div>${esc(order.deliveryPhone || '')}</div>
      <div>${esc(order.deliveryAddress || '')}</div>
      <div class="gov">${esc(order.deliveryGovernorat || '')}</div>
    </div>
    <div class="cod">À ENCAISSER (COD) : <b>${dt(order.total)}</b></div>
    <table class="items">${items}</table>
    <div class="foot">Expéditeur : ${esc(shopName || '')} · Imprimé le ${now()}</div>`;

  const html = `<!doctype html><html lang="fr"><head><meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1"><title>Bordereau ${esc(order.reference)}</title>
<style>
 *{box-sizing:border-box}
 body{font-family:'Segoe UI',system-ui,sans-serif;color:#111;margin:0 auto;padding:16px;width:100mm;max-width:100%}
 .row{display:flex;justify-content:space-between;align-items:center;gap:8px}
 .shop{font-size:18px;font-weight:800}
 .tag{font-size:10px;font-weight:700;letter-spacing:1px;border:1px solid #111;border-radius:4px;padding:2px 6px}
 .ean{margin-top:10px;text-align:center;font-family:'Courier New',monospace;font-size:26px;font-weight:800;letter-spacing:3px;border:2px solid #111;border-radius:6px;padding:10px}
 hr{border:none;border-top:1px dashed #999;margin:10px 0}
 table{width:100%;border-collapse:collapse;font-size:13px}
 .kv td{padding:2px 0}.kv .k{color:#666;width:90px}
 .box{margin:10px 0;border:1px solid #ccc;border-radius:8px;padding:10px}
 .lbl{font-size:10px;font-weight:700;letter-spacing:.5px;color:#666;text-transform:uppercase}
 .big{font-size:16px;font-weight:700;margin-top:2px}
 .gov{font-weight:600}
 .cod{margin:10px 0;text-align:center;font-size:15px;font-weight:700;background:#111;color:#fff;border-radius:6px;padding:8px}
 .items td{padding:2px 0;border-top:1px solid #eee;font-size:12px}.items .r{text-align:right;white-space:nowrap}
 .foot{margin-top:12px;text-align:center;font-size:10px;color:#666}
 @media print{@page{margin:0}body{padding:8px}}
</style></head><body>${body}
<script>window.onload=function(){window.focus();window.print();};window.onafterprint=function(){window.close();};</script>
</body></html>`;
  const w = window.open('', '_blank', 'width=440,height=720');
  if (!w) { alert('Autorisez les fenêtres pop-up pour imprimer le bordereau.'); return; }
  w.document.open(); w.document.write(html); w.document.close();
}
