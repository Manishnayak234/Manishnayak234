# Tub Garden

A day-by-day growth simulator for one container of cucumber (khira) and garden pea,
written as a single self-contained HTML file. Open `index.html` in any browser —
no build, no server, no network needed beyond the webfonts.

It was built around one real setup: seeds from a 10 g packet each of cucumber and
pea, sown into IFFCO Magic Soil mixed with seaweed-fortified vermicompost, in a
repurposed 2 kg whey-protein tub.

## What it does

Every day of the season is computed from the day before it. Nothing on the page is
an animation on a timer — the drawing, the dates, the alerts and the charts all come
out of the same state.

```
weather ──► radiation & reference ET ──► water balance ──► N mineralisation
                     │                         │                  │
                     └──► intercepted light ───┴──► potential DM ──┴──► stress ──►
                          actual DM ──► allocation ──► nodes, leaf area, vine length
                                                  └──► flowers ──► fruit ──► harvest
```

## The model

| Process | Method |
|---|---|
| Weather | IMD 1991–2020 monthly normals for 12 Indian cities, smoothed between month centres, plus seeded day-to-day variability (reproducible: same settings → same season) |
| Radiation | Extraterrestrial radiation from latitude and day of year (FAO-56 eq. 21); solar radiation and reference ET by Hargreaves from the daily temperature range |
| Light at the tub | Open-sky DLI × a site factor (terrace 0.92, half-day balcony 0.42, bright window 0.14, indoor room 0.045) |
| Water | Single-layer bucket. Container capacity minus permanent wilting point sets the available pool; irrigation, rain, drainage and ETc move it. Without drainage holes, water above capacity perches as standing water and starves the roots of oxygen |
| Nitrogen | Organic N in the vermicompost mineralises at ~0.3 %/day at 25 °C, Q10 = 2, slowed when dry or waterlogged; crop demand from biomass × tissue N; peas cover ~58 % of their own demand by fixation after nodulation |
| Carbon | Beer's law interception (k = 0.85) on a canopy footprint capped by the lit area available; RUE 0.78 g DM/mol intercepted PAR (cucumber), 0.66 (pea) |
| Architecture | Nodes are paid for in leaf area, with leaf size rising as the plant matures; internodes stretch in poor light and shorten under water stress |
| Phenology | Growing degree days from sowing — base 10 °C for cucumber, 4.4 °C for pea, daily high capped at the crop's cut-off |
| Fruit | Flowers appear per node. Cucumber needs an insect or a hand; pea is self-pollinating and aborts above ~32 °C. Set fruit competes for assimilate and the youngest drop when there is not enough |
| Stress | Temperature, water, nitrogen, root volume, oxygen and disease all act as multipliers on the same daily carbon gain |
| Pests | Powdery mildew (cucumber) and aphids (pea) from a weather-driven conduciveness score, with a seeded chance that they arrive at all |

## Reference behaviour

One cucumber, 12 L, open terrace, drainage holes, hand-pollinated, sown 1 April in Delhi:

* emergence day 3, first true leaf day 7, vining day 15, first female flower day 29, first pick day 42
* 345 cm of vine, ~50 nodes, ~7,400 cm² of leaf, ~77 g of dry matter
* ~1 kg of fruit over the run, and a daily water demand that passes what a 12 L tub holds

Four peas, 7 L, open terrace, sown 6 October in Delhi: first pick day 59, ~145 g of pods per plant.

## Testing it against the real plant

The panel at the bottom takes your own measurements — date, crop, vine length, leaf
count. Readings are drawn over the model's curve and scored for mean absolute error
and bias. A consistent bias points at an input (usually the light setting or the
sowing date); scatter without bias is the model's own noise. Readings and settings
are kept in browser `localStorage` only.

## Known limits

Climate normals are not your weather. Variety is not modelled — a parthenocarpic
cucumber needs no pollination, a dwarf pea stops at 45 cm where a tall one runs to
1.8 m. Root restriction is one empirical term. Damping-off, transplant shock and
pigeons are not in it at all.
