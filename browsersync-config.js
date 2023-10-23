module.exports = {
    proxy: "localhost:8080",
    logLevel: "debug",
    files: [
        // Need to use polling rather than fsevents, because the parent directory is deleted then recreated
        // Despite this, watching target rather than src because compilation is not instant on src changing
        {
            match: ["target/**/classes/views/**/*.*"],
            options: { usePolling: true }
        },
        {
            match: ["target/**/classes/**/http/**/*.*"],
            options: { usePolling: true }
        },
    ],
    reloadDelay: 1000
};
