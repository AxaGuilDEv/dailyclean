

/*const timezoneOffsetHour = () => {
    var x = new Date();
    return x.getTimezoneOffset() / 60;
}*/

export const getLocalHour=(hourUTC) => {
    const date = new Date();
    date.setHours(hourUTC);
    return date.getHours();
}

export const getUTCHour=(hourLocal) => {
    const date = new Date();
    date.setHours(hourLocal);
    return date.getHours()
}

