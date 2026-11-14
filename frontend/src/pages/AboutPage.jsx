export default function AboutPage() {
  return (
    <main className="page narrow-text">
      <h1>About LASTDROP</h1>
      <p>
        Every day, bakeries, grocers and restaurants throw away good food simply because it is close to its
        freshness deadline. LASTDROP turns that last hour into a sale.
      </p>
      <h3>How the Dutch auction works</h3>
      <p>
        A shop lists a product with a starting price, a minimum price, a drop amount and an interval. Our server
        lowers the price automatically - for example ₹350 → ₹325 → ₹300, one step every hour - until someone
        reserves it, it sells out, it reaches its minimum price, or its deadline passes. Every phone sees the same
        price at the same moment.
      </p>
      <h3>Who it is for</h3>
      <ul>
        <li><strong>Shops</strong> recover part of the value of stock that would otherwise be wasted.</li>
        <li><strong>Buyers</strong> get good food at a discount close to home.</li>
        <li><strong>NGOs</strong> can rescue food in bulk for people who need it.</li>
      </ul>
      <h3>Good to know</h3>
      <ul>
        <li>Payment is Cash on Pickup. This version takes no online payments.</li>
        <li>Food only. Medicines are not allowed on the platform.</li>
        <li>Environmental figures are estimates based on stated assumptions, not measured results.</li>
      </ul>
    </main>
  );
}
