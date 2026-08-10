// Impression d'un ticket client (vente ou réservation) via une fenêtre pop-up au format
// « ticket de caisse » (~80 mm), déclenchée par un clic utilisateur (pas de blocage pop-up).
// Aucune dépendance : on écrit un petit document HTML autonome qui s'auto-imprime.

const esc = (s) => String(s ?? '').replace(/[&<>]/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;' }[c]));
const dt = (x) => `${Number(x || 0).toFixed(3)} DT`;                       // dinar tunisien, 3 décimales
const fmt = (x) => { try { return new Date(x).toLocaleString('fr-FR', { dateStyle: 'short', timeStyle: 'short' }); } catch { return ''; } };

function itemsRows(items) {
  return (items || [])
    .map((it) => `<tr><td>${esc(it.productName)}<div class="muted">${esc(it.colorName)}/${esc(it.size)} × ${it.quantity}</div></td>`
      + `<td class="r">${dt(it.totalPrice ?? it.unitPrice)}</td></tr>`)
    .join('');
}

function render(title, body) {
  const html = `<!doctype html><html lang="fr"><head><meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1"><title>${esc(title)}</title>
<style>
 *{box-sizing:border-box}
 body{font-family:'Segoe UI',system-ui,-apple-system,sans-serif;color:#111;margin:0 auto;padding:12px;width:80mm;max-width:100%}
 .c{text-align:center}.b{font-weight:700}.r{text-align:right;white-space:nowrap}
 .shop{font-size:17px;font-weight:800;letter-spacing:.4px}
 .muted{color:#666;font-size:12px}
 hr{border:none;border-top:1px dashed #999;margin:8px 0}
 table{width:100%;border-collapse:collapse;font-size:12px}
 td{padding:3px 0;vertical-align:top}
 .tot{font-size:15px;font-weight:800}
 .foot{margin-top:12px;text-align:center;font-size:11px;color:#666}
 @media print{@page{margin:0}body{padding:6px}}
</style></head><body>${body}
<script>window.onload=function(){window.focus();window.print();};window.onafterprint=function(){window.close();};</script>
</body></html>`;
  const w = window.open('', '_blank', 'width=380,height=640');
  if (!w) { alert('Autorisez les fenêtres pop-up pour imprimer le ticket.'); return; }
  w.document.open(); w.document.write(html); w.document.close();
}

/** Ticket de caisse d'une vente (SaleResponse : items, discount, totalAmount, change, sellerName). */
export function printSaleTicket(shopName, sale) {
  const body = `
    <div class="c shop">${esc(shopName || 'Boutique')}</div>
    <div class="c muted">Ticket de caisse</div>
    <hr>
    <div class="b">Vente #${esc(sale.id)}</div>
    <div class="muted">${fmt(sale.saleDate)}</div>
    ${sale.sellerName ? `<div class="muted">Vendeur : ${esc(sale.sellerName)}</div>` : ''}
    <hr>
    <table>${itemsRows(sale.items)}</table>
    <hr>
    <table>
      ${Number(sale.discount) > 0 ? `<tr><td>Remise</td><td class="r">- ${dt(sale.discount)}</td></tr>` : ''}
      <tr><td class="tot">TOTAL</td><td class="r tot">${dt(sale.totalAmount)}</td></tr>
      ${sale.paymentMethod ? `<tr><td>Paiement</td><td class="r">${esc(sale.paymentMethod)}</td></tr>` : ''}
      ${Number(sale.change) > 0 ? `<tr><td>Rendu monnaie</td><td class="r">${dt(sale.change)}</td></tr>` : ''}
    </table>
    <div class="foot">Merci de votre visite 🛍️<br>Imprimé le ${fmt(new Date())}</div>`;
  render(`Vente #${sale.id}`, body);
}

/** Bon de réservation (acompte) — total / payé / reste + coordonnées client. */
export function printReservationTicket(shopName, r) {
  const body = `
    <div class="c shop">${esc(shopName || 'Boutique')}</div>
    <div class="c muted">Bon de réservation</div>
    <hr>
    <div class="b">${esc(r.reference)}</div>
    <div class="muted">${esc(r.customerName || '')}${r.customerPhone ? ` · ${esc(r.customerPhone)}` : ''}</div>
    <div class="muted">Créée le ${fmt(r.createdAt)}</div>
    <hr>
    <table>${itemsRows(r.items)}</table>
    <hr>
    <table>
      <tr><td>Total</td><td class="r">${dt(r.total)}</td></tr>
      <tr><td>Payé</td><td class="r">${dt(r.paid)}</td></tr>
      <tr><td class="tot">Reste à payer</td><td class="r tot">${dt(r.remaining)}</td></tr>
    </table>
    <div class="foot">Merci ! Conservez ce bon pour le retrait.<br>Imprimé le ${fmt(new Date())}</div>`;
  render(String(r.reference || 'Réservation'), body);
}
