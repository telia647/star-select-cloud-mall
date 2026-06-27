ALTER TABLE pms_product
    ADD COLUMN main_image VARCHAR(255) NULL AFTER subtitle,
    ADD COLUMN gallery_images JSON NULL AFTER main_image;

UPDATE pms_product
SET main_image = '/demo-products/phone.svg',
    gallery_images = JSON_ARRAY('/demo-products/phone.svg', '/demo-products/headset.svg', '/demo-products/notebook.svg')
WHERE id = 2001;

UPDATE pms_product
SET main_image = '/demo-products/notebook.svg',
    gallery_images = JSON_ARRAY('/demo-products/notebook.svg', '/demo-products/backpack.svg', '/demo-products/speaker.svg')
WHERE id = 2002;

UPDATE pms_product
SET main_image = '/demo-products/headset.svg',
    gallery_images = JSON_ARRAY('/demo-products/headset.svg', '/demo-products/speaker.svg', '/demo-products/phone.svg')
WHERE id = 2010;

UPDATE pms_product
SET main_image = '/demo-products/kettle.svg',
    gallery_images = JSON_ARRAY('/demo-products/kettle.svg', '/demo-products/aroma.svg', '/demo-products/cleanser.svg')
WHERE id = 2011;

UPDATE pms_product
SET main_image = '/demo-products/serum.svg',
    gallery_images = JSON_ARRAY('/demo-products/serum.svg', '/demo-products/cleanser.svg', '/demo-products/aroma.svg')
WHERE id = 2012;

UPDATE pms_product
SET main_image = '/demo-products/running-shoes.svg',
    gallery_images = JSON_ARRAY('/demo-products/running-shoes.svg', '/demo-products/backpack.svg', '/demo-products/scarf.svg')
WHERE id = 2013;

UPDATE pms_product
SET main_image = '/demo-products/aroma.svg',
    gallery_images = JSON_ARRAY('/demo-products/aroma.svg', '/demo-products/kettle.svg', '/demo-products/serum.svg')
WHERE id = 2014;

UPDATE pms_product
SET main_image = '/demo-products/backpack.svg',
    gallery_images = JSON_ARRAY('/demo-products/backpack.svg', '/demo-products/notebook.svg', '/demo-products/running-shoes.svg')
WHERE id = 2015;

UPDATE pms_product
SET main_image = '/demo-products/speaker.svg',
    gallery_images = JSON_ARRAY('/demo-products/speaker.svg', '/demo-products/headset.svg', '/demo-products/phone.svg')
WHERE id = 2016;

UPDATE pms_product
SET main_image = '/demo-products/scarf.svg',
    gallery_images = JSON_ARRAY('/demo-products/scarf.svg', '/demo-products/backpack.svg', '/demo-products/running-shoes.svg')
WHERE id = 2017;

UPDATE pms_product
SET main_image = '/demo-products/notebook.svg',
    gallery_images = JSON_ARRAY('/demo-products/notebook.svg', '/demo-products/phone.svg', '/demo-products/backpack.svg')
WHERE id = 2018;

UPDATE pms_product
SET main_image = '/demo-products/cleanser.svg',
    gallery_images = JSON_ARRAY('/demo-products/cleanser.svg', '/demo-products/serum.svg', '/demo-products/aroma.svg')
WHERE id = 2019;
